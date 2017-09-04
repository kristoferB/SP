package spgui.widgets.services

import scala.util.{Try}
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

import spgui.widgets.services.{APIServiceHandler => api}


object ServiceWidgetComm {
  def extractResponse(m: SPMessage) = for {
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[api.Response]
  } yield (h, b)

  def makeMess(h: SPHeader, b: api.Request) = SPMessage.make[SPHeader, api.Request](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.make[SPHeader, APISP](h, b)
}

