package sp.runners

import sp.domain._
import sp.domain.Logic._
import scala.util._


object APIOperationRunner {
  sealed trait Request
  sealed trait Response
  val service = "OperationRunner"

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

  implicit lazy val fSetup: JSFormat[Setup] = deriveFormatISA[Setup]
  object Request {
    implicit lazy val fExampleServiceRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fExampleServiceResponse: JSFormat[Response] = deriveFormatISA[Response]
  }
}

object OperationRunnerInfo {
  case class OperationRunnerRequest(request: APIOperationRunner.Request)
  case class OperationRunnerResponse(response: APIOperationRunner.Response)

  val req: com.sksamuel.avro4s.SchemaFor[OperationRunnerRequest] = com.sksamuel.avro4s.SchemaFor[OperationRunnerRequest]
  val resp: com.sksamuel.avro4s.SchemaFor[OperationRunnerResponse] = com.sksamuel.avro4s.SchemaFor[OperationRunnerResponse]

  val apischema = makeMeASchema(
    req(),
    resp()
  )

  val attributes: APISP.StatusResponse = APISP.StatusResponse(
    service = APIOperationRunner.service,
    instanceID = Some(ID.newID),
    instanceName = "",
    tags = List("runtime", "operations", "runner"),
    api = apischema, //SPAttributes.fromJson(s().toString).get,
    version = 1,
    attributes = SPAttributes.empty
  )
}

import sp.runners.{APIAbilityHandler => abilityAPI}


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
    b <- m.getBodyAs[abilityAPI.AbilityState] if abilities.contains(b.id)
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





