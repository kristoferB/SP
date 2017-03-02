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
import Pickles._

import scala.util.{Failure, Success, Try}




// to be able to use opcua runtime api
package APIVirtualDevice {
  sealed trait Requests
  // requests setup
  case class SetUpDeviceDriver(driver: Driver) extends Requests
  case class SetUpResource(resource: Resource) extends Requests

  // Add new transformers here as needed.
  sealed trait DriverStateMapper
  case class OneToOneMapper(driver: String, key: String) extends DriverStateMapper
  case class OneToOneNestedMapper(driver: String, key: List[String]) extends DriverStateMapper
  case class OneToOneNewKeyMapper(driver: String, key: String, newKey: String) extends DriverStateMapper


  // requests command (gets a SPACK and when applied, SPDone (and indirectly a StateEvent))
  case class ResourceCommand(resource: String, stateRequest: Map[String, SPValue], timeout: Int = 0) extends Requests

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



class VirtualDevice(name: String, id: UUID) extends PersistentActor with ActorLogging {
  override def persistenceId = id.toString

  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)


  override def receiveCommand = {
    case x: String =>


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


trait VirtualDeviceLogic extends VDMappers{
  val name: String
  val id: UUID

  case class Resource(r: api.Resource, read: List[StateMapper], write: List[StateMapper])
  case class StateMapper(f: (Map[String, StateMap], Map[String, StateMap]) =>  Map[String, StateMap])


  var drivers: Map[String, api.Driver] = Map()
  var driverState: Map[String, StateMap] = Map()
  var resources: Map[String, Resource] = Map()
  var resourceState: Map[String, StateMap] = Map()

  var driverToResMapper: List[StateMapper] = List()
  var resToDriverMapper: List[StateMapper] = List()


  private val defaultWriter = StateMapper{ case (res, drivers) =>
    val x = writeAllPrefix(res(name))
    writeTo(x, drivers)
  }

  private val defaultReader = StateMapper{ case (drivers, res) =>
    val x = readAllPrefix(drivers, name)
    writeTo(x, res)
  }



  private val defResource = Resource(
    api.Resource(name, id, List(), SPAttributes, false),
    List(defaultReader), List(defaultWriter))

  resources += name -> defResource





  def newDriver(d: api.Driver) = {
    drivers += d.name -> d
  }

  def newResource(resource: api.Resource) = {

  }

  def driverEvent(e: api.DriverStateChange) = {
    val current = driverState.get(e.name)
    current.foreach{sm =>
      val upd = sm ++ e.state
      driverState +   e.name -> upd
    }
    current.foreach{ds =>
      resources.map{r =>
        r._2.read.foreach {mapper =>
          resourceState = mapper.f(ds, resourceState)
        }
      }
    }

  }


}

trait VDDriverComm {
  val context: ActorContext
  import context.dispatcher
  val mediator: ActorRef


}

trait VDMappers {
  type StateMap = Map[String, SPValue]

  def writeTo(upd: Map[String, StateMap], current: Map[String, StateMap]): Map[String, StateMap] = {
    current.map{kv =>
      val updM = upd.getOrElse(kv._1, Map())
      kv._1 -> (kv._2 ++ updM)
    }
  }

  def readAllPrefix(thingState: Map[String, StateMap], resource: String): Map[String, StateMap]  = {
    Map(resource -> flatMapState(thingState))
  }

  def writeAllPrefix(thingState: StateMap) = {
    extractPrefixState(thingState)
  }

  def copyValue(thingState: Map[String, StateMap], from: (String, String), to: (String, String)) = {
    val v = for {m <- thingState.get(from._1); value <- m.get(from._2)} yield value
    val updKV = v.map(x => Map(to._2 -> x)).getOrElse(Map())
    to._1 -> updKV
  }

  private def flatMapState(thingState: Map[String, StateMap]) = {
    thingState.foldLeft(Map[String, SPValue]()) { (a, b) =>
      a ++ prefixState(b._2, b._1)
    }
  }

  private def prefixState(state: StateMap, prefix: String) = state.map(kv => s"$prefix.${kv._1}" -> kv._2)

  private def extractPrefixState(state: StateMap) = {
    state.groupBy{kv =>
      kv._1.split(".").headOption
    }.collect{case (Some(str), x) => str -> x}
  }

}
