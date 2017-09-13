package sp.opcua

import java.util.UUID

import scala.util.{Failure, Success}

import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import sp.domain._
import Logic._



import sp.devicehandler.{APIVirtualDevice => vdapi}

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
      SPMessage.fromJson(x).foreach{mess =>
          for {
            h <- mess.getHeaderAs[SPHeader]
            b <- mess.getBodyAs[vdapi.Request]
          } yield {
            b match {
              case vdapi.SetUpDeviceDriver(d) if d.driverType == opcUADriver =>
                context.actorOf(OpcUARuntime.props(d.name, d.id, d.setup), d.id.toString())
              case _ =>
            }
          }
      }
    case _ => sender ! APISP.SPError("Ill formed request")
  }
}


