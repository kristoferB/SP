package sp.models

import sp.domain._
import Logic._

import scala.util.Try


object APIModel {
  sealed trait Request
  sealed trait Response
  val service = "Model"

  case class CreateModel(name: String, attributes: SPAttributes = SPAttributes(), id: ID = ID.newID) extends Request
  case class DeleteModel(id: ID) extends Request
  case class UpdateModelAttributes(name: Option[String], attributes: Option[SPAttributes]) extends Request
  case class RevertModel(toVersion: Int) extends Request
  case class Import(name: String, id: ID, version: Int, attributes: SPAttributes = SPAttributes(), items: List[IDAble]) extends Request

  case class GetModel() extends Request
  case class GetModels() extends Request
  case class GetModelInfo() extends Request
  case class GetModelHistory() extends Request

  case class GetItems() extends Request
  case class GetItem(itemID: ID) extends Request
  case class GetItemsInList(items: List[ID]) extends Request
  case class GetStructures() extends Request
  // add more messages here later

  case class PutItems(items: List[IDAble], info: SPAttributes = SPAttributes()) extends Request
  case class DeleteItems(items: List[ID], info: SPAttributes = SPAttributes()) extends Request

  case class TheModel(name: String, id: ID, version: Int, attributes: SPAttributes = SPAttributes(), items: List[IDAble]) extends Response
  case class ModelInformation(name: String, id: ID, version: Int, attributes: SPAttributes = SPAttributes()) extends Response
  case class ModelDeleted(model: ID) extends Response
  case class ModelUpdate(model: ID, version: Int, updatedItems: List[IDAble] = List(), deletedItems: List[ID] = List(), info: SPAttributes = SPAttributes()) extends Response
  case class ModelHistory(model: ID, history: List[(Int, SPAttributes)]) extends Response
  case class ModelList(models: List[ID]) extends Response
  case class SPItem(item: IDAble) extends Response
  case class SPItems(items: List[IDAble]) extends Response

  object Request {
    implicit lazy val fExampleServiceRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fExampleServiceResponse: JSFormat[Response] = deriveFormatISA[Response]
  }

}



object ModelInfo {
  case class ModelRequest(request: APIModel.Request)
  case class ModelResponse(response: APIModel.Response)

  val req: com.sksamuel.avro4s.SchemaFor[ModelRequest] = com.sksamuel.avro4s.SchemaFor[ModelRequest]
  val resp: com.sksamuel.avro4s.SchemaFor[ModelResponse] = com.sksamuel.avro4s.SchemaFor[ModelResponse]

  val apischema = makeMeASchema(
    req(),
    resp()
  )

  val attributes: APISP.StatusResponse = APISP.StatusResponse(
    service = APIModel.service,
    tags = List("model"),
    api = apischema,
    version = 1,
    attributes = SPAttributes.empty
  )
}

import sp.models.{APIModel => api}


object ModelsComm {
  def extractRequest(mess: Try[SPMessage], name: String,  id: ID): Option[(SPHeader, api.Request)] = for {
    m <- mess.toOption
    h <- m.getHeaderAs[SPHeader].toOption if h.to == id.toString || h.to == name
    b <- m.getBodyAs[api.Request].toOption
  } yield (h, b)

  def extractAPISP(mess: Try[SPMessage]): Option[(SPHeader, APISP)] = for {
    m <- mess.toOption
    h <- m.getHeaderAs[SPHeader].toOption
    b <- m.getBodyAs[APISP].toOption if b == APISP.StatusRequest
  } yield (h, b)


  def makeMess(h: SPHeader, b: api.Response) = SPMessage.makeJson[SPHeader, api.Response](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)
}
