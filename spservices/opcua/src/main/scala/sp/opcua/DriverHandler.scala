package sp.opcua

import java.util.UUID

import scala.util.{Failure, Success}

import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import sp.domain._
import sp.messages.APISP
import sp.messages.Pickles._

package APIVirtualDevice {
  sealed trait Requests
  // requests setup
  case class SetUpDeviceDriver(driver: Driver) extends Requests

  // requests from driver
  case class DriverStateChange(name: String, id: UUID, state: Map[String, SPValue], diff: Boolean = false) extends Requests
  case class DriverCommand(name: String, id: UUID, state: Map[String, SPValue]) extends Requests
  case class DriverCommandDone(requestID: UUID, result: Boolean) extends Requests

  // answers
  sealed trait Replies
  case class NewDriver(x: Driver) extends Replies
  case class RemovedDriver(x: Driver) extends Replies

  case class Driver(name: String, id: UUID, driverType: String, setup: SPAttributes)


  object  attributes {
    val service = "virtualDevice"
  }
}
import sp.opcua.{APIVirtualDevice => vdapi}

object DriverHandler {
  def props = Props(classOf[DriverHandler])
}

class DriverHandler extends Actor {
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("driverCommands", self)

  val opcUADriver = "OPCUA"

  def receive = {
    case "stop" =>
      context.children.foreach { child =>
        child ! "disconnect"
      }

    case x: String =>
      println(x)
      SPMessage.fromJson(x) match {
        case Success(mess) =>
          for {
            h <- mess.getHeaderAs[SPHeader]
            b <- mess.getBodyAs[vdapi.Requests]
          } yield {
            b match {
              case vdapi.SetUpDeviceDriver(d) if d.driverType == opcUADriver =>
                context.actorOf(OpcUARuntime.props(d.name, d.id, d.setup), d.id.toString())
              case _ =>
            }
          }
        case Failure(err) =>
      }
    case _ => sender ! APISP.SPError("Ill formed request")
  }
}
