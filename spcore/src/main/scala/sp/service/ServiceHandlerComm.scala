package sp.service


import sp.domain._
import sp.messages._
import Pickles._
import scala.util.{Try}

package APIServiceHandler {
  sealed trait Request
  case class GetServices() extends Request

  sealed trait Response
  case class Services(xs: List[APISP.StatusResponse]) extends Response
  case class NewService(x: APISP.StatusResponse) extends Response
  case class RemovedService(x: APISP.StatusResponse) extends Response

  object attributes {
    val service = "ServiceHandler"
  }
}

import sp.service.{APIServiceHandler => api}



object ServiceHandlerComm {
  def extractRequest(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] if h.to == api.attributes.service
    b <- m.getBodyAs[api.Request]
  } yield (h, b)

  def extractAPISP(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP]
  } yield (h, b)


  def makeMess(h: SPHeader, b: api.Response) = SPMessage.makeJson[SPHeader, api.Response](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)
}
