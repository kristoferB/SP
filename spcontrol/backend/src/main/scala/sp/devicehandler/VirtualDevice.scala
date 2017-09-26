package sp.devicehandler

import java.util.UUID
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._

import akka.actor._
import akka.persistence._
import sp.domain._
import sp.domain.Logic._


object VirtualDevice {
  def props(name: String, id: UUID) = Props(classOf[VirtualDevice], name, id)
}

object VirtualDeviceInfo {
  import sp.domain.SchemaLogic._

  case class VirtualDeviceRequest(request: APIVirtualDevice.Request)
  case class VirtualDeviceResponse(response: APIVirtualDevice.Response)

  val req: com.sksamuel.avro4s.SchemaFor[VirtualDeviceRequest] = com.sksamuel.avro4s.SchemaFor[VirtualDeviceRequest]
  val resp: com.sksamuel.avro4s.SchemaFor[VirtualDeviceResponse] = com.sksamuel.avro4s.SchemaFor[VirtualDeviceResponse]

  val apischema = makeMeASchema(
    req(),
    resp()
  )

  private val id = ID.newID
  val attributes: APISP.StatusResponse = APISP.StatusResponse(
    service = "VirtualDevice",
    instanceID = Some(id),
    instanceName = s"VD-$id",
    tags = List("virtual device", "vd", "runtime", "communication"),
    api = apischema,
    version = 1,
    attributes = SPAttributes.empty
  )
}



class VirtualDevice(val name: String, val id: UUID) extends PersistentActor
    with ActorLogging
    with VirtualDeviceLogic
    with sp.service.ServiceCommunicationSupport
    with sp.service.MessageBussSupport {
  import sp.devicehandler.{APIVirtualDevice => api}

  override def persistenceId = id.toString

  import context.dispatcher

  subscribe(api.topicRequest)
  subscribe("driverEvents")

    // Setting up the status response that is used for identifying the service in the cluster
  val statusResponse = VirtualDeviceInfo.attributes.copy(
    instanceID = Some(id)
  )
  // starts wiating for ping requests from service handler
  triggerServiceRequestComm(statusResponse)

  override def receiveCommand = {
    case x: String =>
      val mess = SPMessage.fromJson(x)

      for {
        m <- mess
        h <- m.getHeaderAs[SPHeader]
        b <- m.getBodyAs[api.Request]
      } yield {
        b match {
          case api.SetUpDeviceDriver(d) =>
            println("new driver " + d)
            newDriver(d)
            publish("driverCommands", x)

          case api.SetUpResource(r) =>
            println("new resource " + r)
            newResource(r)
            sendResources

          case e @ api.DriverStateChange(name, did, state, _) =>
            //println("got a statechange:" + e)
            val oldrs = resourceState
            driverEvent(e)
            //println("new driver state: " + driverState)
            //println("new resource state: " + resourceState)

            resourceState.filter { case (nid, ns) =>
              oldrs.get(nid) match {
                case Some(os) => (ns.toSet diff os.toSet).nonEmpty
                case None => true
              }
            }.foreach { case (rid, state) if resources.contains(rid) =>
              val header = SPHeader(from = id.toString)
              val body = api.StateEvent(resources(rid).r.name, rid, state)
              // hur ska vi ha det med event/answer-topics?
              // publish("events", SPMessage.makeJson(header, body))
              publish(api.topicResponse, SPMessage.makeJson(header, body))
            }

          case api.DriverCommandDone(reqid, success) =>
            val request = activeDriverRequests.filter { case (rid,reqs) => reqs.contains(reqid) }
            activeDriverRequests = request.headOption match {
              case Some((rid,reqs)) =>
                if(!success) {
                  val errorHeader = SPHeader(reqID = reqid, from = VirtualDeviceInfo.attributes.service)
                  publish(api.topicResponse, SPMessage.makeJson(errorHeader, APISP.SPError("driver command failed")))
                  activeDriverRequests - rid
                } else {
                  val nr = reqs.filter(_!=reqid)
                  if(nr.isEmpty) {
                    val doneHeader = SPHeader(reqID = reqid, from = VirtualDeviceInfo.attributes.service)
                    publish(api.topicResponse, SPMessage.makeJson(doneHeader, APISP.SPDone()))
                    activeDriverRequests - rid
                  } else {
                    activeDriverRequests + (rid -> nr)
                  }
                }
              case None => activeDriverRequests
            }

          case r : api.ResourceCommand =>
            val ackHeader = h.copy(reply = VirtualDeviceInfo.attributes.service)
            publish(api.topicResponse, SPMessage.makeJson(ackHeader, APISP.SPACK()))

            val diffs = getDriverDiffs(r)

            val doneHeader = h.copy(reply = VirtualDeviceInfo.attributes.service)
            if(diffs.isEmpty || diffs.forall { case (k,v) => v.isEmpty }) {
              println("No variables to update... Sending done immediately for requst: " + h.reqID)
              publish(api.topicResponse, SPMessage.makeJson(doneHeader, APISP.SPDone()))
            } else {
              val commands = getDriverCommands(diffs)
              val requests = commands.map { command =>
                // send commands to the drivers
                val header = SPHeader(from = id.toString)
                publish("driverCommands", SPMessage.makeJson(header, command))
                header.reqID
              }
              activeDriverRequests += (h.reqID -> requests.toList)
              // start timeout counter
              if(r.timeout > 0) {
                val dct = DriverCommandTimeout(h.reqID, r.timeout)
                context.system.scheduler.scheduleOnce(Duration(r.timeout, TimeUnit.MILLISECONDS), self, dct)
              }
            }
          case api.GetResources =>
            sendResources

          case x => println("todo: " + x)
        }
      }

    case DriverCommandTimeout(request, timeout) =>
      activeDriverRequests.get(request).map { reqs =>
        println("Driver command(s) timed out after " + timeout + "ms")
        println(" failed driver requests: " + reqs.mkString(", "))
      }
      activeDriverRequests = activeDriverRequests - request
  }

  def sendResources = {
    val h = SPHeader(from = id.toString)
    val b = api.Resources(resources.values.toList.map(_.r))
    publish(api.topicResponse, SPMessage.makeJson(h, b))
  }

  def receiveRecover = {
    case x: String =>

    case RecoveryCompleted =>

  }
}


trait VirtualDeviceLogic {
  val name: String
  val id: UUID

  case class DriverCommandTimeout(requestID: UUID, timeout: Int)

  case class StateReader(f: (Map[UUID, DriverState], Map[UUID, ResourceState]) =>  Map[UUID, ResourceState])
  case class StateWriter(f: (Map[UUID, ResourceState], Map[UUID, DriverState]) =>  Map[UUID, DriverState])

  case class Resource(r: APIVirtualDevice.Resource, read: List[StateReader], write: List[StateWriter])

  type ResourceState = Map[UUID, SPValue]
  type DriverState = Map[String, SPValue]

  var drivers: Map[UUID, APIVirtualDevice.Driver] = Map()
  var driverState: Map[UUID, DriverState] = Map()
  var activeDriverRequests: Map[UUID, List[UUID]] = Map()

  var resources: Map[UUID, Resource] = Map()
  var resourceState: Map[UUID, ResourceState] = Map()

  def newDriver(d: APIVirtualDevice.Driver) = {
    drivers += d.id -> d
    driverState += d.id -> Map[String, SPValue]()
  }

  def newResource(resource: APIVirtualDevice.Resource) = {
    val rw = resource.stateMap.flatMap {
      case APIVirtualDevice.OneToOneMapper(t, id, name) =>
        val reader = StateReader{ case (drivers, resources) =>
          val nr = for {
            driver <- drivers.get(id)
            value <- driver.get(name)
            rs <- resources.get(resource.id)
          } yield {
            resources + (resource.id -> (rs + (t -> value)))
          }
          nr.getOrElse(resources)
        }
        val writer = StateWriter{ case (resources, drivers) =>
          val nd = for {
            rs <- resources.get(resource.id)
            value <- rs.get(t)
            driver <- drivers.get(id)
          } yield {
            drivers + (id -> (driver + (name -> value)))
          }
          nd.getOrElse(drivers)
        }
        Some((reader,writer))
      case _ => None // potentially add other mapping types
    }

    resources += resource.id -> Resource(resource, rw.map(_._1), rw.map(_._2))
    resourceState += resource.id -> Map()
  }

  def driverEvent(e: APIVirtualDevice.DriverStateChange) = {
    val current = driverState.get(e.id)
    current.foreach{ state =>
      val upd = state ++ e.state
      driverState += e.id -> upd
    }
    resourceState = resources.foldLeft(resourceState){ case (rs, r) =>
      r._2.read.foldLeft(rs){ case (rs, reader) => reader.f(driverState, rs)}}
  }

  def getDriverDiffs(c: APIVirtualDevice.ResourceCommand) = {
    val diffs = for {
      r <- resources.get(c.resource)
    } yield {
      val s = r.write.foldLeft(driverState) { case (ds,writer) =>
        writer.f(Map(c.resource -> c.stateRequest), ds)
      }
      s.map { case (k,v) =>
        val m = driverState.getOrElse(k, v)
        val d = v.toSet diff m.toSet
        (k, d.toMap)
      }
    }
    diffs.getOrElse(Map())
  }

  def getDriverCommands(diffs: Map[UUID, DriverState]) = {
    for {
      (did,stateDiff) <- diffs if stateDiff.nonEmpty
      d <- drivers.get(did)
    } yield {
      APIVirtualDevice.DriverCommand(d.name, did, stateDiff)
    }
  }
}
