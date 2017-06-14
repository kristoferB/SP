package sp.abilityhandler

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._
import scala.util._

package APIAbilityHandler {
  sealed trait Request

  case class StartAbility(id: ID, params: Map[ID, SPValue] = Map(), attributes: SPAttributes = SPAttributes()) extends Request
  case class ForceResetAbility(id: ID) extends Request
  case class ForceResetAllAbilities() extends Request

  // to be used when handshake is on
  case class ExecuteCmd(cmd: ID) extends Request

  case class GetAbilities() extends Request
  case class SetUpAbility(ability: Ability, handshake: Boolean = false) extends Request


  sealed trait Response

  case class CmdID(cmd: ID) extends Response
  case class AbilityStarted(id: ID) extends Response
  case class AbilityCompleted(id: ID, result: Map[ID, SPValue]) extends Response
  case class AbilityState(id: ID, state: Map[ID, SPValue]) extends Response
  case class Abilities(xs: List[Ability]) extends Response
  case class Abs(a: List[(ID,String)]) extends Response

  case class Ability(name: String,
                     id: ID,
                     preCondition: Condition = Condition(AlwaysFalse, List()),
                     started: Condition = Condition(AlwaysFalse, List()),
                     postCondition: Condition = Condition(AlwaysTrue, List()),
                     resetCondition: Condition = Condition(AlwaysTrue, List()),
                     parameters: List[ID] = List(),
                     result: List[ID] = List(),
                     attributes: SPAttributes = SPAttributes()) extends Response

  object attributes {
    val service = "abilityHandler"
  }
}

import sp.abilityhandler.{APIAbilityHandler => api}
import sp.devicehandler.{APIVirtualDevice => vdAPI}


object AbilityComm {
  def extractRequest(mess: Try[SPMessage], instanceID: ID, name: String) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader] if h.to == instanceID.toString || h.to == name || h.to == api.attributes.service
      b <- m.getBodyAs[api.Request]
    } yield (h, b)

  def extractVDReply(mess: Try[SPMessage], instanceID: ID, vd: String) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] if h.from.contains(vd) || h.reply == SPValue(instanceID)
    b <- m.getBodyAs[vdAPI.Replies]
    } yield (h, b)

  def extractServiceRequest(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP.StatusRequest]
    } yield (h, b)


  def makeMess(h: SPHeader, b: api.Response) = SPMessage.makeJson[SPHeader, api.Response](h, b)
  def makeMess(h: SPHeader, b: vdAPI.Requests) = SPMessage.makeJson[SPHeader, vdAPI.Requests](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)

}
