package spgui.widgets.services

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

import spgui.widgets.services.{APIServiceHandler => api}


object ServiceWidgetComm {
  def extractResponse(m: SPMessage) = for {
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[api.Response]
  } yield (h, b)

  def makeMess(h: SPHeader, b: api.Request) = SPMessage.make[SPHeader, api.Request](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.make[SPHeader, APISP](h, b)
}

