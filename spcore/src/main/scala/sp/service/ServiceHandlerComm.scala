package sp.service


import sp.domain._
import Logic._
import scala.util.Try




case object GetServices extends APIServiceHandler.Request
case class Services(xs: List[APISP.StatusResponse]) extends APIServiceHandler.Response
case class NewService(x: APISP.StatusResponse) extends APIServiceHandler.Response
case class RemovedService(x: APISP.StatusResponse) extends APIServiceHandler.Response

object APIServiceHandler {
  sealed trait Request
  sealed trait Response
  object Request {
    implicit lazy val readReq = deriveReadISA[Request]
    implicit lazy val writeReq = deriveWriteISA[Request]
  }
  object Response {
    implicit lazy val readResp = deriveReadISA[Response]
    implicit lazy val writeResp = deriveWriteISA[Response]
  }

  object attributes {
    val service = "ServiceHandler"
  }
}




object ServiceHandlerComm {
  def extractRequest(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] if h.to == "ServiceHandler"
    b <- m.getBodyAs[APIServiceHandler.Request]
  } yield (h, b)

  def extractAPISP(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP]
  } yield (h, b)


  def makeMess(h: SPHeader, b: APIServiceHandler.Response) = SPMessage.makeJson(h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson(h, b)
}
