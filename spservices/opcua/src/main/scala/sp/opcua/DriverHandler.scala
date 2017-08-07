package sp.opcua

import java.util.UUID

import scala.util.{Failure, Success}

import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import sp.domain._
import Logic._



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
            b <- mess.getBodyAs[vdapi.Request]
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


object APIVirtualDevice {
  sealed trait Request
  // requests setup
  case class SetUpDeviceDriver(driver: Driver) extends Request
//  case class SetUpResource(resource: Resource) extends Request
//  case class GetResources() extends Request
//
//  sealed trait DriverStateMapper
//  case class OneToOneMapper(thing: UUID, driverID: UUID, driverIdentifier: String) extends DriverStateMapper
//
//  // requests command (gets a SPACK and when applied, SPDone (and indirectly a StateEvent))
//  case class ResourceCommand(resource: UUID, stateRequest: Map[UUID, SPValue], timeout: Int = 0) extends Request

  // requests from driver
  case class DriverStateChange(name: String, id: UUID, state: Map[String, SPValue], diff: Boolean = false) extends Request
  case class DriverCommand(name: String, id: UUID, state: Map[String, SPValue]) extends Request
  case class DriverCommandDone(requestID: UUID, result: Boolean) extends Request

  // answers
  sealed trait Response
  case class StateEvent(resource: String, id: UUID, state: Map[UUID, SPValue], diff: Boolean = false) extends Response

//  case class Resources(xs: List[Resource]) extends Response
//  case class Drivers(xs: List[Driver]) extends Response
//  case class NewResource(x: Resource) extends Response
//  case class RemovedResource(x: Resource) extends Response
//  case class NewDriver(x: Driver) extends Response
//  case class RemovedDriver(x: Driver) extends Response
//
//  case class Resource(name: String, id: UUID, things: Set[UUID], stateMap: List[DriverStateMapper], setup: SPAttributes, sendOnlyDiffs: Boolean = false)
  case class Driver(name: String, id: UUID, driverType: String, setup: SPAttributes)


  implicit lazy val fDriver: JSFormat[Driver] = deriveFormatISA[Driver]
  implicit lazy val fDriverCommand: JSFormat[DriverCommand] = deriveFormatISA[DriverCommand]
//  implicit lazy val fResource: JSFormat[Resource] = deriveFormatISA[Resource]
  object Request {
    implicit lazy val fVirtualDeviceRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fVirtualDeviceResponse: JSFormat[Response] = deriveFormatISA[Response]
  }


}