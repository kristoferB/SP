package sp.abilityhandler

import sp.domain.Logic._
import sp.domain._

  object APIAbilityHandler {
    sealed trait Request
    sealed trait Response
    val service = "abilityHandler"
    val topicRequest = "abilityHandlerRequest"
    val topicResponse = "abilityHandlerRespponse"

    final case class StartAbility(id: ID, params: Map[ID, SPValue] = Map(), attributes: SPAttributes = SPAttributes()) extends Request
    final case class ForceResetAbility(id: ID) extends Request
    case object ForceResetAllAbilities extends Request

    // to be used when handshake is on
    final case class ExecuteCmd(cmd: ID) extends Request

    case object GetAbilities extends Request
    final case class SetUpAbility(ability: Ability, handshake: Boolean = false) extends Request



    final case class CmdID(cmd: ID) extends Response
    final case class AbilityStarted(id: ID) extends Response
    final case class AbilityCompleted(id: ID, result: Map[ID, SPValue]) extends Response
    final case class AbilityState(id: ID, state: Map[ID, SPValue]) extends Response
    final case class Abilities(xs: List[Ability]) extends Response
    final case class Abs(a: List[(ID,String)]) extends Response

    final case class Ability(name: String,
                             id: ID,
                             preCondition: Condition = Condition(AlwaysFalse, List()),
                             started: Condition = Condition(AlwaysFalse, List()),
                             postCondition: Condition = Condition(AlwaysTrue, List()),
                             resetCondition: Condition = Condition(AlwaysTrue, List()),
                             parameters: List[ID] = List(),
                             result: List[ID] = List(),
                             attributes: SPAttributes = SPAttributes())




    object Formats {
      import play.api.libs.json._
      implicit lazy val fAbility: JSFormat[Ability] = Json.format[Ability]
      implicit lazy val fStartAbility: JSFormat[StartAbility] = Json.format[StartAbility]
      implicit lazy val fForceResetAbility: JSFormat[ForceResetAbility] = Json.format[ForceResetAbility]
      implicit lazy val fForceResetAllAbilities:     JSFormat[ForceResetAllAbilities.type]     = deriveCaseObject[ForceResetAllAbilities.type]
      implicit lazy val fExecuteCmd: JSFormat[ExecuteCmd] = Json.format[ExecuteCmd]
      implicit lazy val fGetAbilities:     JSFormat[GetAbilities.type]     = deriveCaseObject[GetAbilities.type]
      implicit lazy val fSetUpAbility: JSFormat[SetUpAbility] = Json.format[SetUpAbility]
      implicit lazy val fCmdID: JSFormat[CmdID] = Json.format[CmdID]
      implicit lazy val fAbilityStarted: JSFormat[AbilityStarted] = Json.format[AbilityStarted]
      implicit lazy val fAbilityCompleted: JSFormat[AbilityCompleted] = Json.format[AbilityCompleted]
      implicit lazy val fAbilityState: JSFormat[AbilityState] = Json.format[AbilityState]
      implicit lazy val fAbilities: JSFormat[Abilities] = Json.format[Abilities]
      implicit lazy val fAbs: JSFormat[Abs] = Json.format[Abs]
      def fAbilityHandlerRequest: JSFormat[Request] = Json.format[Request]
      def fAbilityHandlerResponse: JSFormat[Response] = Json.format[Response]
    }


    object Request {
      implicit lazy val fAPIAbilityHandlerRequest: JSFormat[Request] = Formats.fAbilityHandlerRequest
    }

    object Response {
      implicit lazy val fAPIAbilityHandlerResponse: JSFormat[Response] = Formats.fAbilityHandlerResponse
    }
  }
