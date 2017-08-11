package sp.devicehandler

import sp.domain._
import sp.domain.Logic._

object APIVirtualDevice {
  sealed trait Request
  sealed trait Response
  val service = "VirtualDevice"

  // requests setup
  case class SetUpDeviceDriver(driver: Driver) extends Request
  case class SetUpResource(resource: Resource) extends Request
  case class GetResources() extends Request

  sealed trait DriverStateMapper
  case class OneToOneMapper(thing: ID, driverID: ID, driverIdentifier: String) extends DriverStateMapper

  // requests command (gets a SPACK and when applied, SPDone (and indirectly a StateEvent))
  case class ResourceCommand(resource: ID, stateRequest: Map[ID, SPValue], timeout: Int = 0) extends Request

  // requests from driver
  case class DriverStateChange(name: String, id: ID, state: Map[String, SPValue], diff: Boolean = false) extends Request
  case class DriverCommand(name: String, id: ID, state: Map[String, SPValue]) extends Request
  case class DriverCommandDone(requestID: ID, result: Boolean) extends Request

  // answers
  case class StateEvent(resource: String, id: ID, state: Map[ID, SPValue], diff: Boolean = false) extends Response

  case class Resources(xs: List[Resource]) extends Response
  case class Drivers(xs: List[Driver]) extends Response
  case class NewResource(x: Resource) extends Response
  case class RemovedResource(x: Resource) extends Response
  case class NewDriver(x: Driver) extends Response
  case class RemovedDriver(x: Driver) extends Response

  case class Resource(name: String, id: ID, things: Set[ID], stateMap: List[DriverStateMapper], setup: SPAttributes, sendOnlyDiffs: Boolean = false)
  case class Driver(name: String, id: ID, driverType: String, setup: SPAttributes)


  implicit lazy val fDriver: JSFormat[Driver] = deriveFormatISA[Driver]
  implicit lazy val fResource: JSFormat[Resource] = deriveFormatISA[Resource]
  object Request {
    implicit lazy val fVirtualDeviceRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fVirtualDeviceResponse: JSFormat[Response] = deriveFormatISA[Response]
  }
  object DriverStateMapper {
    implicit lazy val fDriverStateMapper: JSFormat[DriverStateMapper] = deriveFormatISA[DriverStateMapper]
  }

}


object VirtualDeviceInfo {
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
