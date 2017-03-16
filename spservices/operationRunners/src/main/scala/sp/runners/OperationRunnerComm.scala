package sp.runners

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._
import scala.util._


package API_OperationRunner {
  sealed trait Request
  sealed trait Reply

  case class Setup(name: String, runnerID: ID, ops: Set[Operation], opAbilityMap: Map[ID, ID], initialState: Map[ID, SPValue]) extends Request
  case class SetState(runnerID: ID, state: Map[ID, SPValue]) extends Request
  case class ForceComplete(ability: ID) extends Request
  case class TerminateRunner(runnerID: ID) extends Request
  case class GetState(runnerID: ID) extends Request
  case class GetRunners() extends Request

  case class StateEvent(runnerID: ID, state: Map[ID, SPValue]) extends Reply
  case class Runners(ids: List[Setup]) extends Reply


  object attributes {
    val service = "OperationRunner"
    val version = 1.0
    val api = "to be fixed by macros"
  }
}

import sp.runners.{API_OperationRunner => api}
import sp.runners.{APIAbilityHandler => abilityAPI}


object OperationRunnerComm {
  def extractRequest(mess: Try[SPMessage]) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader] if h.to == api.attributes.service
      b <- m.getBodyAs[api.Request]
    } yield (h, b)

  def extractAbilityReply(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] if h.reply == SPValue(api.attributes.service)
    b <- m.getBodyAs[abilityAPI.Response]
    } yield (h, b)

  def extractAbilityStates(mess: Try[SPMessage], abilities: Set[ID]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[abilityAPI.AbilityState] if abilities.contains(b.id)
    } yield (h, b)

  def extractServiceRequest(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP.StatusRequest]
    } yield (h, b)


  def makeMess(h: SPHeader, b: api.Reply) = SPMessage.makeJson[SPHeader, api.Reply](h, b)
  def makeMess(h: SPHeader, b: abilityAPI.Request) = SPMessage.makeJson[SPHeader, abilityAPI.Request](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)

}





package APIAbilityHandler {
  sealed trait Request
  sealed trait Response

  case class StartAbility(id: ID, params: Map[ID, SPValue] = Map(), attributes: SPAttributes = SPAttributes()) extends Request
  case class ForceResetAbility(id: ID) extends Request

  case class AbilityStarted(id: ID) extends Response
  case class AbilityCompleted(id: ID, result: Map[ID, SPValue]) extends Response
  case class AbilityState(id: ID, state: Map[ID, SPValue]) extends Response


  object attributes {
    val service = "abilityHandler"
  }
}