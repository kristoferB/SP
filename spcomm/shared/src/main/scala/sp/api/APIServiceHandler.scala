package sp.service


import sp.domain._
import Logic._


object APIServiceHandler {
  sealed trait Request
  sealed trait Response
  val service = "ServiceHandler"

  case object GetServices extends Request
  case class Services(xs: List[APISP.StatusResponse]) extends Response
  case class NewService(x: APISP.StatusResponse) extends Response
  case class RemovedService(x: APISP.StatusResponse) extends Response


  object Request {
    implicit lazy val fServiceHandlerRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fServiceHandlerResponse: JSFormat[Response] = deriveFormatISA[Response]
  }
}
