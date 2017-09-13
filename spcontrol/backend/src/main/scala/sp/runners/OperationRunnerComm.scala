package sp.runners

import sp.domain._
import sp.domain.Logic._
import scala.util._





import sp.abilityhandler.{APIAbilityHandler => abilityAPI}


object OperationRunnerInfo {

  import sp.domain.SchemaLogic._

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

object OperationRunnerComm {
  def extractRequest(mess: Option[SPMessage]) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader] if h.to == APIOperationRunner.service
      b <- m.getBodyAs[APIOperationRunner.Request]
    } yield (h, b)

  def extractAbilityReply(mess: Option[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] // if h.reply == SPValue(APIOperationRunner.attributes.service)
    b <- m.getBodyAs[abilityAPI.Response]
    } yield (h, b)

  def extractAbilityStates(mess: Option[SPMessage], abilities: Set[ID]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[abilityAPI.Response] if b.isInstanceOf[abilityAPI.AbilityState] && abilities.contains(b.asInstanceOf[abilityAPI.AbilityState].id)
    } yield (h, b)

  def extractServiceRequest(mess: Option[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP] if b == APISP.StatusRequest
    } yield (h, b)


  def makeMess(h: SPHeader, b: APIOperationRunner.Response) = SPMessage.makeJson[SPHeader, APIOperationRunner.Response](h, b)
  def makeMess(h: SPHeader, b: abilityAPI.Request) = SPMessage.makeJson[SPHeader, abilityAPI.Request](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)

}





