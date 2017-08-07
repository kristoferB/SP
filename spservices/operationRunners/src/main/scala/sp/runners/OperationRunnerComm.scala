package sp.runners

import sp.domain._
import sp.domain.Logic._
import scala.util._


object API_OperationRunner {
  sealed trait Request
  sealed trait Response
  val service = "OperationRunner"

  case class Setup(name: String, runnerID: ID, ops: Set[Operation], opAbilityMap: Map[ID, ID], initialState: Map[ID, SPValue]) extends Request
  case class SetState(runnerID: ID, state: Map[ID, SPValue]) extends Request
  case class AddOperations(runnerID: ID, ops: Set[Operation], opAbilityMap: Map[ID, ID]) extends Request
  case class RemoveOperations(runnerID: ID, ops: Set[ID]) extends Request
  case class ForceComplete(ability: ID) extends Request
  case class TerminateRunner(runnerID: ID) extends Request
  case class GetState(runnerID: ID) extends Request
  case class GetRunners() extends Request

  case class StateEvent(runnerID: ID, state: Map[ID, SPValue]) extends Response
  case class Runners(ids: List[Setup]) extends Response


  import play.api.libs.json._

  implicit lazy val ididReads: JSReads[Map[ID, ID]] = new JSReads[Map[ID, ID]] {
    override def reads(json: JsValue): JsResult[Map[ID, ID]] = {
      json.validate[Map[String, String]].map(xs => xs.collect{case (k, v) if ID.isID(k) && ID.isID(v) => ID.makeID(k).get -> ID.makeID(v).get})
    }
  }
  implicit lazy val ididWrites: JSWrites[Map[ID, ID]] = new OWrites[Map[ID, ID]] {
    override def writes(xs: Map[ID, ID]): JsObject = {
      val toFixedMap = xs.map{case (k, v) => k.toString -> SPValue(v)}
      JsObject(toFixedMap)
    }
  }
  implicit lazy val fSetup: JSReads[Setup] = deriveReadISA[Setup]
  object Request {
    implicit lazy val fExampleServiceRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fExampleServiceResponse: JSFormat[Response] = deriveFormatISA[Response]
  }
}

object OperationRunnerInfo {
  case class Schema(request: API_OperationRunner.Request, response: API_OperationRunner.Response)
  val s: com.sksamuel.avro4s.SchemaFor[Schema] = com.sksamuel.avro4s.SchemaFor[Schema]

  val attributes: APISP.StatusResponse = APISP.StatusResponse(
    service = API_OperationRunner.service,
    instanceID = Some(ID.newID),
    instanceName = "",
    tags = List("runtime", "operations", "runner"),
    api = SPAttributes.fromJson(s().toString).get,
    version = 1,
    attributes = SPAttributes.empty
  )
}

import sp.runners.{API_OperationRunner => api}
import sp.runners.{APIAbilityHandler => abilityAPI}


object OperationRunnerComm {
  def extractRequest(mess: Try[SPMessage]) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader] if h.to == api.service
      b <- m.getBodyAs[api.Request]
    } yield (h, b)

  def extractAbilityReply(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] // if h.reply == SPValue(api.attributes.service)
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
    m.getBodyAs[APISP] if b == APISP.StatusRequest
    } yield (h, b)


  def makeMess(h: SPHeader, b: api.Response) = SPMessage.makeJson[SPHeader, api.Response](h, b)
  def makeMess(h: SPHeader, b: abilityAPI.Request) = SPMessage.makeJson[SPHeader, abilityAPI.Request](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)

}





object APIAbilityHandler {
  sealed trait Request
  sealed trait Response
  val service = "abilityHandler"

  case class StartAbility(id: ID, params: Map[ID, SPValue] = Map(), attributes: SPAttributes = SPAttributes()) extends Request
  case class ForceResetAbility(id: ID) extends Request
  case class ForceResetAllAbilities() extends Request

  // to be used when handshake is on
  case class ExecuteCmd(cmd: ID) extends Request

  case class GetAbilities() extends Request
  case class SetUpAbility(ability: Ability, handshake: Boolean = false) extends Request



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


  implicit lazy val fAbility: JSReads[Ability] = deriveReadISA[Ability]
  implicit lazy val fAbilityState: JSReads[AbilityState] = deriveReadISA[AbilityState]
  object Request {
    implicit lazy val fExampleServiceRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fExampleServiceResponse: JSFormat[Response] = deriveFormatISA[Response]
  }
}
