package sp.abilityhandler

import sp.domain._
import sp.domain.Logic._
import scala.util._



import sp.abilityhandler.{APIAbilityHandler => api}
import sp.devicehandler.{APIVirtualDevice => vdAPI}


object AbilityComm {
  def extractRequest(mess: Option[SPMessage], instanceID: ID, name: String) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader] if h.to == instanceID.toString || h.to == name || h.to == AbilityHandler.attributes.service
      b <- m.getBodyAs[api.Request]
    } yield (h, b)

  def extractVDReply(mess: Option[SPMessage], instanceID: ID, vd: String) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] if h.from.contains(vd) || h.reply == SPValue(instanceID)
    b <- m.getBodyAs[vdAPI.Response]
    } yield (h, b)

  def extractServiceRequest(mess: Option[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP] if b == APISP.StatusRequest
    } yield {
    (h, b)
  }


  def makeMess(h: SPHeader, b: api.Response) = SPMessage.makeJson[SPHeader, api.Response](h, b)
  def makeMess(h: SPHeader, b: vdAPI.Request) = SPMessage.makeJson[SPHeader, vdAPI.Request](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)

}


