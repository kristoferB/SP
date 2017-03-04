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




// to be able to use opcua runtime api
package APIVirtualDevice {
  sealed trait Requests
  // requests setup
  case class SetUpDeviceDriver(driver: Driver) extends Requests
  case class SetUpResource(resource: Resource) extends Requests

  // Add new transformers here as needed.
  sealed trait DriverStateMapper
  case class OneToOneMapper(thing: UUID, driverID: UUID, driverIdentifier: String) extends DriverStateMapper

  // requests command (gets a SPACK and when applied, SPDone (and indirectly a StateEvent))
  case class ResourceCommand(resource: UUID, stateRequest: Map[UUID, SPValue], timeout: Int = 0) extends Requests

  // requests from driver
  case class DriverStateChange(name: String, id: UUID, state: Map[String, SPValue], diff: Boolean = false)  extends Requests
  case class DriverCommand(name: String, id: UUID, state: Map[String, SPValue]) extends Requests

  // answers
  sealed trait Replies
  case class StateEvent(resource: String, id: UUID, state: Map[String, SPValue], diff: Boolean = false) extends Replies

  case class Resources(xs: List[Resource]) extends Replies
  case class Drivers(xs: List[Driver]) extends Replies
  case class NewResource(x: Resource) extends Replies
  case class RemovedResource(x: Resource) extends Replies
  case class NewDriver(x: Driver) extends Replies
  case class RemovedDriver(x: Driver) extends Replies


  case class Resource(name: String, id: UUID, stateMap: List[DriverStateMapper], setup: SPAttributes, sendOnlyDiffs: Boolean = false)
  case class Driver(name: String, id: UUID, driverType: String, setup: SPAttributes)


  object attributes {
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
            driverEvent(e)
            println("new driver state: " + driverState)
            println("new resource state: " + resourceState)

          case r : api.ResourceCommand =>
            println("resource command: " + r)
            val msgs = resourceCommand(r)
            msgs.foreach { m => mediator ! Publish("driverCommands", m.toJson) }

          case x => println("todo: " + x)
        }
      }


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

  case class StateReader(f: (Map[UUID, DriverState], Map[UUID, ResourceState]) =>  Map[UUID, ResourceState])
  case class StateWriter(f: (Map[UUID, ResourceState], Map[UUID, DriverState]) =>  Map[UUID, DriverState])

  case class Resource(r: api.Resource, read: List[StateReader], write: List[StateWriter])

  type ResourceState = Map[UUID, SPValue]
  type DriverState = Map[String, SPValue]

  var drivers: Map[UUID, api.Driver] = Map()
  var driverState: Map[UUID, DriverState] = Map()
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

  def resourceCommand(c: api.ResourceCommand) = {
    val diffs = (for {
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
    }).getOrElse(Map())
    for {
      (did,stateDiff) <- diffs if stateDiff.nonEmpty
      d <- drivers.get(did)
      header = SPHeader(from = name)
      body = api.DriverCommand(d.name, did, stateDiff)
      msg <- SPMessage.make(header, body).toOption
    } yield {
      msg
    }
  }
}
