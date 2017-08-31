package sp.runners

import sp.domain._
import sp.domain.Logic._
import scala.util._





import sp.abilityhandler.{APIAbilityHandler => abilityAPI}


object OperationRunnerComm {
  def extractRequest(mess: Try[SPMessage]) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader] if h.to == APIOperationRunner.service
      b <- m.getBodyAs[APIOperationRunner.Request]
    } yield (h, b)

  def extractAbilityReply(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] // if h.reply == SPValue(APIOperationRunner.attributes.service)
    b <- m.getBodyAs[abilityAPI.Response]
    } yield (h, b)

  def extractAbilityStates(mess: Try[SPMessage], abilities: Set[ID]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[abilityAPI.Response] if b.isInstanceOf[abilityAPI.AbilityState] && abilities.contains(b.asInstanceOf[abilityAPI.AbilityState].id)
    } yield (h, b)

  def extractServiceRequest(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP] if b == APISP.StatusRequest
    } yield (h, b)


  def makeMess(h: SPHeader, b: APIOperationRunner.Response) = SPMessage.makeJson[SPHeader, APIOperationRunner.Response](h, b)
  def makeMess(h: SPHeader, b: abilityAPI.Request) = SPMessage.makeJson[SPHeader, abilityAPI.Request](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)

}





