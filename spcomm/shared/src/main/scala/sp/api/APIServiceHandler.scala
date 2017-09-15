package sp.service {


import sp.domain._
import Logic._


object APIServiceHandler {
  sealed trait Request
  sealed trait Response
  val service = "ServiceHandler"
  val topicRequest = "serviceRequest"
  val topicResponse = "serviceResponse"

  case object GetServices extends Request
  case class RemoveService(x: APISP.StatusResponse) extends Request

  case class Services(xs: List[APISP.StatusResponse]) extends Response
  case class ServiceAdded(x: APISP.StatusResponse) extends Response
  case class ServiceRemoved(x: APISP.StatusResponse) extends Response


  object Formats {
    import play.api.libs.json._

    implicit val fGetServices : JSFormat[GetServices.type] = deriveCaseObject[GetServices.type]
    implicit val fRemoveService : JSFormat[RemoveService] = Json.format[RemoveService]
    implicit val fCreateModel: JSFormat[Services] = Json.format[Services]
    implicit val fNewService: JSFormat[ServiceAdded] = Json.format[ServiceAdded]
    implicit val fRemovedService: JSFormat[ServiceRemoved] = Json.format[ServiceRemoved]
    def defServiceHandlerRequest: JSFormat[Request] = Json.format[Request]
    def defServiceHandlerResponse: JSFormat[Response] = Json.format[Response]
  }

  object Request {
    implicit lazy val fServiceHandlerRequest: JSFormat[Request] = Formats.defServiceHandlerRequest
  }
  object Response {
    implicit lazy val fServiceHandlerResponse: JSFormat[Response] = Formats.defServiceHandlerResponse
  }
}

}
