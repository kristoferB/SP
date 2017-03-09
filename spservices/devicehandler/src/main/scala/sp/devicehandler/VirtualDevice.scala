package sp.devicehandler

import java.util.UUID

import akka.actor._
import sp.domain._
import sp.domain.Logic._

import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import akka.persistence._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put, Subscribe}
import java.util.concurrent.TimeUnit

import org.joda.time.DateTime
import sp.messages._
import sp.messages.Pickles._

import scala.util.{Failure, Success, Try}

package APIVirtualDevice {
  sealed trait Requests
  // requests setup
  case class SetUpDeviceDriver(driver: Driver) extends Requests
  case class SetUpResource(resource: Resource) extends Requests
  case class GetResources() extends Requests

  sealed trait DriverStateMapper
  case class OneToOneMapper(thing: UUID, driverID: UUID, driverIdentifier: String) extends DriverStateMapper

  // requests command (gets a SPACK and when applied, SPDone (and indirectly a StateEvent))
  case class ResourceCommand(resource: UUID, stateRequest: Map[UUID, SPValue], timeout: Int = 0) extends Requests

  // requests from driver
  case class DriverStateChange(name: String, id: UUID, state: Map[String, SPValue], diff: Boolean = false) extends Requests
  case class DriverCommand(name: String, id: UUID, state: Map[String, SPValue]) extends Requests
  case class DriverCommandDone(requestID: UUID, result: Boolean) extends Requests

  // answers
  sealed trait Replies
  case class StateEvent(resource: String, id: UUID, state: Map[UUID, SPValue], diff: Boolean = false) extends Replies

  case class Resources(xs: List[Resource]) extends Replies
  case class Drivers(xs: List[Driver]) extends Replies
  case class NewResource(x: Resource) extends Replies
  case class RemovedResource(x: Resource) extends Replies
  case class NewDriver(x: Driver) extends Replies
  case class RemovedDriver(x: Driver) extends Replies

  case class Resource(name: String, id: UUID, things: List[UUID], stateMap: List[DriverStateMapper], setup: SPAttributes, sendOnlyDiffs: Boolean = false)
  case class Driver(name: String, id: UUID, driverType: String, setup: SPAttributes)


  object  attributes {
    val service = "virtualDevice"
  }
}
import sp.devicehandler.{APIVirtualDevice => api}


object VirtualDevice {
  def props(name: String, id: UUID) = Props(classOf[VirtualDevice], name, id)
}



class VirtualDevice(val name: String, val id: UUID) extends PersistentActor with ActorLogging with VirtualDeviceLogic {
  override def persistenceId = id.toString

  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("driverEvents", self)

  override def receiveCommand = {
    case x: String =>
      val mess = SPMessage.fromJson(x)

      for {
        m <- mess
        h <- m.getHeaderAs[SPHeader]
        b <- m.getBodyAs[api.Requests]
      } yield {
        b match {
          case api.SetUpDeviceDriver(d) =>
            println("new driver " + d)
            newDriver(d)
            mediator ! Publish("driverCommands", x)

          case api.SetUpResource(r) =>
            println("new resource " + r)
            newResource(r)

          case e @ api.DriverStateChange(name, id, state, _) =>
            println("got a statechange:" + e)
            val oldrs = resourceState
            driverEvent(e)
            println("new driver state: " + driverState)
            println("new resource state: " + resourceState)

            resourceState.filter { case (nid, ns) =>
              oldrs.get(nid) match {
                case Some(os) => (ns.toSet diff os.toSet).nonEmpty
                case None => true
              }
            }.foreach { case (rid, state) if resources.contains(rid) =>
              val header = SPHeader(from = name, fromID = Some(id))
              val body = api.StateEvent(resources(rid).r.name, rid, state)
              mediator ! Publish("spevents", SPMessage.makeJson(header, body))
            }

            val finishedRequests = checkRequestsFinished() // mutates state
            finishedRequests.foreach { header =>
              println("sending request done for request: " + header.reqID)
              mediator ! Publish("answers", SPMessage.makeJson(header, APISP.SPDone()))
            }


          case api.DriverCommandDone(reqid, success) =>
            val request = activeDriverRequests.filter { case (rid,reqs) => reqs.contains(reqid) }
            activeDriverRequests = request.headOption match {
              case Some((rid,reqs)) =>
                if(!success) {
                  val errorHeader = SPHeader(reqID = reqid, from = api.attributes.service)
                  mediator ! Publish("answers", SPMessage.makeJson(errorHeader, APISP.SPError("driver command failed")))
                  activeDriverRequests - rid
                } else {
                  val nr = reqs.filter(_!=reqid)
                  if(nr.isEmpty) {
                    val doneHeader = SPHeader(reqID = reqid, from = api.attributes.service)
                    mediator ! Publish("answers", SPMessage.makeJson(doneHeader, APISP.SPDone()))
                    activeDriverRequests - rid
                  } else {
                    activeDriverRequests + (rid -> nr)
                  }
                }
              case None => activeDriverRequests
            }

          case r : api.ResourceCommand =>
            println("resource command: " + r + " with request id: " + h.reqID)
            val ackHeader = h.copy(replyFrom = api.attributes.service)
            mediator ! Publish("answers", SPMessage.makeJson(ackHeader, APISP.SPACK()))

            val diffs = getDriverDiffs(r)

            val doneHeader = h.copy(replyFrom = api.attributes.service)
            if(diffs.isEmpty || diffs.forall { case (k,v) => v.isEmpty }) {
              println("No variables to update... Sending done immediately for requst: " + h.reqID)
              mediator ! Publish("answers", SPMessage.makeJson(doneHeader, APISP.SPDone()))
            } else {
              val commands = getDriverCommands(diffs)
              val requests = commands.map { command =>
                // send commands to the drivers
                val header = SPHeader(from = name)
                mediator ! Publish("driverCommands", SPMessage.makeJson(header, command))
                header.reqID
              }
              activeDriverRequests += (h.reqID -> requests.toList)
              // start timeout counter
              if(r.timeout > 0) {
                val dct = DriverCommandTimeout(h.reqID, r.timeout)
                context.system.scheduler.scheduleOnce(Duration(r.timeout, TimeUnit.MILLISECONDS), self, dct)
              }
            }
          case x: api.GetResources =>
            val updh = h.copy(replyFrom = name, replyFromID = Some(id))
            val b = api.Resources(resources.values.toList.map(_.r))
            mediator ! Publish("answers", m.makeJson(updh, b))

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

  def receiveRecover = {
    case x: String =>

    case RecoveryCompleted =>

  }

  val statusResponse = SPAttributes(
    "service" -> name,
    "instanceID" -> id,
    "groups" -> List("devices"),
    "attributes" -> api.attributes
  )

}


trait VirtualDeviceLogic {
  val name: String
  val id: UUID

  case class DriverCommandTimeout(requestID: UUID, timeout: Int)

  case class StateReader(f: (Map[UUID, DriverState], Map[UUID, ResourceState]) =>  Map[UUID, ResourceState])
  case class StateWriter(f: (Map[UUID, ResourceState], Map[UUID, DriverState]) =>  Map[UUID, DriverState])

  case class Resource(r: api.Resource, read: List[StateReader], write: List[StateWriter])

  type ResourceState = Map[UUID, SPValue]
  type DriverState = Map[String, SPValue]

  var drivers: Map[UUID, api.Driver] = Map()
  var driverState: Map[UUID, DriverState] = Map()
  var driverRequests: Map[SPHeader, Map[UUID, DriverState]] = Map()
  var activeDriverRequests: Map[UUID, List[UUID]] = Map()

  var resources: Map[UUID, Resource] = Map()
  var resourceState: Map[UUID, ResourceState] = Map()

  def newDriver(d: api.Driver) = {
    drivers += d.id -> d
    driverState += d.id -> Map[String, SPValue]()
  }

  def newResource(resource: api.Resource) = {
    val rw = resource.stateMap.map {
      case api.OneToOneMapper(t, id, name) =>
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
    }.flatten

    resources += resource.id -> Resource(resource, rw.map(_._1), rw.map(_._2))
    resourceState += resource.id -> Map()
  }

  def driverEvent(e: api.DriverStateChange) = {
    val current = driverState.get(e.id)
    current.foreach{ state =>
      val upd = state ++ e.state
      driverState += e.id -> upd
    }
    resourceState = resources.foldLeft(resourceState){ case (rs, r) =>
      r._2.read.foldLeft(rs){ case (rs, reader) => reader.f(driverState, rs)}}
  }

  def getDriverDiffs(c: api.ResourceCommand) = {
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
      api.DriverCommand(d.name, did, stateDiff)
    }
  }

  def updateDriverRequests(header: SPHeader, diffs: Map[UUID, DriverState]) = {
    if(driverRequests.contains(header)) {
      // check that header does not already exist - dont do that!
      println("Request already made!")
      None.get
    }
    driverRequests += (header -> diffs)
  }

  def checkRequestsFinished(): List[SPHeader] = {
    val updReqs = driverRequests.map { case (h, diffs) =>
      val updDiffs = diffs.map { case (drvID, state) =>
        val drvState = driverState.getOrElse(drvID, Map())
        val diff = state.toSet diff drvState.toSet
        (drvID, diff.toMap)
      }
      (h, updDiffs.filter { case (drvID, state) => state.nonEmpty })
    }
    val completedReqs = updReqs.filter { case (h, diffs) => diffs.isEmpty }.map(_._1)
    val notCompleted = updReqs.filterNot { case (h, diffs) => diffs.isEmpty }

    driverRequests = notCompleted
    completedReqs.toList
  }
}
