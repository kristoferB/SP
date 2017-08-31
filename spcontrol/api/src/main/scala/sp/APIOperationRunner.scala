package sp.runners

import sp.domain._
import sp.domain.Logic._

object APIOperationRunner {
  val service = "OperationRunner"
  sealed trait Request
  sealed trait Response

  case class CreateRunner(setup: Setup) extends Request
  case class SetState(runnerID: ID, state: Map[ID, SPValue]) extends Request
  case class AddOperations(runnerID: ID, ops: Set[Operation], opAbilityMap: Map[ID, ID]) extends Request
  case class RemoveOperations(runnerID: ID, ops: Set[ID]) extends Request
  case class ForceComplete(ability: ID) extends Request
  case class TerminateRunner(runnerID: ID) extends Request
  case class GetState(runnerID: ID) extends Request
  case object GetRunners extends Request

  case class StateEvent(runnerID: ID, state: Map[ID, SPValue]) extends Response
  case class Runners(ids: List[Setup]) extends Response

  case class Setup(name: String, runnerID: ID, ops: Set[Operation], opAbilityMap: Map[ID, ID], initialState: Map[ID, SPValue])


  object Formats {
    import play.api.libs.json._
    implicit lazy val fSetup: JSFormat[Setup] = Json.format[Setup]
    implicit lazy val fCreateRunner: JSFormat[CreateRunner] = Json.format[CreateRunner]
    implicit lazy val fSetState: JSFormat[SetState] = Json.format[SetState]
    implicit lazy val fAddOperations: JSFormat[AddOperations] = Json.format[AddOperations]
    implicit lazy val fRemoveOperations: JSFormat[RemoveOperations] = Json.format[RemoveOperations]
    implicit lazy val fForceComplete: JSFormat[ForceComplete] = Json.format[ForceComplete]
    implicit lazy val fTerminateRunner: JSFormat[TerminateRunner] = Json.format[TerminateRunner]
    implicit lazy val fGetState: JSFormat[GetState] = Json.format[GetState]
    implicit lazy val fGetRunners : JSFormat[GetRunners.type] = deriveCaseObject[GetRunners.type ]
    implicit lazy val fStateEvent: JSFormat[StateEvent] = Json.format[StateEvent]
    implicit lazy val fRunners: JSFormat[Runners] = Json.format[Runners]
    def fOperationRunnerRequest: JSFormat[Request] = Json.format[Request]
    def fOperationRunnerResponse: JSFormat[Response] = Json.format[Response]
  }

  object Request {
    implicit lazy val fOperationRunnerRequest: JSFormat[Request] = Formats.fOperationRunnerRequest
  }

  object Response {
    implicit lazy val fOperationRunnerResponse: JSFormat[Response] = Formats.fOperationRunnerResponse
  }
}
