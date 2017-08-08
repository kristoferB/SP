package sp.abilityhandler

import sp.domain._
import sp.domain.Logic._
import scala.util._

object APIAbilityHandler {
  sealed trait Request
  sealed trait Response
  val service = "abilityHandler"

  case class StartAbility(id: ID, params: Map[ID, SPValue] = Map(), attributes: SPAttributes = SPAttributes()) extends Request
  case class ForceResetAbility(id: ID) extends Request
  case object ForceResetAllAbilities extends Request

  // to be used when handshake is on
  case class ExecuteCmd(cmd: ID) extends Request

  case object GetAbilities extends Request
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
                     attributes: SPAttributes = SPAttributes())


  implicit lazy val fAbility: JSFormat[Ability] = deriveFormatISA[Ability]
  //implicit lazy val fAbilityState: JSReads[AbilityState] = deriveReadISA[AbilityState]
  object Request {
    implicit lazy val fExampleServiceRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fExampleServiceResponse: JSFormat[Response] = deriveFormatISA[Response]
  }
}


object AbilityHandlerInfo {
  case class AbilityHandlerRequest(request: APIAbilityHandler.Request)
  case class AbilityHandlerResponse(response: APIAbilityHandler.Response)

  val sres: com.sksamuel.avro4s.SchemaFor[AbilityHandlerResponse] = com.sksamuel.avro4s.SchemaFor[AbilityHandlerResponse]
  val sreq: com.sksamuel.avro4s.SchemaFor[AbilityHandlerRequest] = com.sksamuel.avro4s.SchemaFor[AbilityHandlerRequest]

  val apischema = makeMeASchema(
    sreq(),
    sres()
  )


  val attributes: APISP.StatusResponse = APISP.StatusResponse(
    service = APIAbilityHandler.service,
    instanceID = Some(ID.newID),
    instanceName = "",
    tags = List("ability", "virtual device", "vd", "runtime", "communication"),
    api = apischema,
    version = 1,
    attributes = SPAttributes.empty
  )
}

import sp.abilityhandler.{APIAbilityHandler => api}
import sp.devicehandler.{APIVirtualDevice => vdAPI}


object AbilityComm {
  def extractRequest(mess: Try[SPMessage], instanceID: ID, name: String) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader] if h.to == instanceID.toString || h.to == name || h.to == AbilityHandlerInfo.attributes.service
      b <- m.getBodyAs[api.Request]
    } yield (h, b)

  def extractVDReply(mess: Try[SPMessage], instanceID: ID, vd: String) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] if h.from.contains(vd) || h.reply == SPValue(instanceID)
    b <- m.getBodyAs[vdAPI.Response]
    } yield (h, b)

  def extractServiceRequest(mess: Try[SPMessage]) = for {
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


