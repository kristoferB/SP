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
package APIDeviceHandler {
  sealed trait APIDeviceHandler
  // requests


  // answers


  object attributes {
    val service = "deviceHandler"
  }
}
import sp.devicehandler.{APIDeviceHandler => api}


object DeviceHandler {
  def props = Props(classOf[DeviceHandler])
}
class DeviceHandler extends PersistentActor with ActorLogging {
  override def persistenceId = "DeviceHandler"
  val id: UUID = UUID.randomUUID()

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
    "service" -> api.attributes.service,
    "instanceID" -> id,
    "groups" -> List("spcore", "devices"),
    "attributes" -> api.attributes
  )

}

