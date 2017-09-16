package sp.devicehandler {

  import sp.domain._
  import sp.domain.Logic._

  object APIVirtualDevice {
    val service = "VirtualDevice"
    val topicRequest = "virtualDeviceRequests"
    val topicResponse = "virtualDeviceResponse"

    sealed trait Request
    sealed trait Response

    sealed trait DriverStateMapper
    case class OneToOneMapper(thing: ID, driverID: ID, driverIdentifier: String) extends DriverStateMapper

    // requests setup
    case class SetUpDeviceDriver(driver: Driver) extends Request
    case class SetUpResource(resource: Resource) extends Request
    case object GetResources extends Request
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


    object Formats {
      import play.api.libs.json._
      implicit lazy val fDriver: JSFormat[Driver] = Json.format[Driver]
      implicit lazy val fResource: JSFormat[Resource] = Json.format[Resource]
      implicit lazy val fOneToOneMapper: JSFormat[OneToOneMapper] = Json.format[OneToOneMapper]
      implicit lazy val fDriverStateMapper: JSFormat[DriverStateMapper] = Json.format[DriverStateMapper]
      implicit lazy val fSetUpDeviceDriver: JSFormat[SetUpDeviceDriver] = Json.format[SetUpDeviceDriver]
      implicit lazy val fSetUpResource:     JSFormat[SetUpResource]     = Json.format[SetUpResource]
      implicit lazy val fGetResources:     JSFormat[GetResources.type]     = deriveCaseObject[GetResources.type]
      implicit lazy val fResourceCommand:     JSFormat[ResourceCommand]     = Json.format[ResourceCommand]
      implicit lazy val fDriverStateChange:     JSFormat[DriverStateChange]     = Json.format[DriverStateChange]
      implicit lazy val fDriverCommand:     JSFormat[DriverCommand]     = Json.format[DriverCommand]
      implicit lazy val fDriverCommandDone:     JSFormat[DriverCommandDone]     = Json.format[DriverCommandDone]
      implicit lazy val fStateEvent:     JSFormat[StateEvent]     = Json.format[StateEvent]
      implicit lazy val fResources:     JSFormat[Resources]     = Json.format[Resources]
      implicit lazy val fDrivers:     JSFormat[Drivers]     = Json.format[Drivers]
      implicit lazy val fNewResource:     JSFormat[NewResource]     = Json.format[NewResource]
      implicit lazy val fRemovedResource:     JSFormat[RemovedResource]     = Json.format[RemovedResource]
      implicit lazy val fNewDriver:     JSFormat[NewDriver]     = Json.format[NewDriver]
      implicit lazy val fRemovedDriver:     JSFormat[RemovedDriver]     = Json.format[RemovedDriver]
      def fVirtualDeviceRequest: JSFormat[Request] = Json.format[Request]
      def fVirtualDeviceResponse: JSFormat[Response] = Json.format[Response]
    }


    object Request {
      implicit lazy val fVirtualDeviceRequest: JSFormat[Request] = Formats.fVirtualDeviceRequest
    }

    object Response {
      implicit lazy val fVirtualDeviceResponse: JSFormat[Response] = Formats.fVirtualDeviceResponse
    }


  }

}
