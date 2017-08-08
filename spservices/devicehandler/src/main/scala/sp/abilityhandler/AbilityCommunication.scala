package sp.abilityhandler

import sp.domain._
import Logic._

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
