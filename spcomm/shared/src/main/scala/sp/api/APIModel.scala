package sp.models {

import sp.domain._
import Logic._

object APIModelMaker {
  sealed trait Request
  sealed trait Response
  val service = "ModelMaker"
  val topicRequest = "modelRequests"
  val topicResponse = "modelResponse"

  case class CreateModel(name: String, attributes: SPAttributes = SPAttributes(), id: ID = ID.newID) extends Request
  case class DeleteModel(id: ID) extends Request
  case object GetModels extends Request

  case class ModelCreated(name: String, attributes: SPAttributes = SPAttributes(), id: ID) extends Response
  case class ModelDeleted(id: ID) extends Response
  case class ModelList(models: List[ID]) extends Response

  object Formats {
    import play.api.libs.json._

    implicit val fCreateModel: JSFormat[CreateModel] = Json.format[CreateModel]
    implicit val fDeleteModel: JSFormat[DeleteModel] = Json.format[DeleteModel]
    implicit val fGetModels : JSFormat[GetModels.type] = deriveCaseObject[GetModels.type]
    implicit val fModelCreated: JSFormat[ModelCreated] = Json.format[ModelCreated]
    implicit val fModelDeleted: JSFormat[ModelDeleted] = Json.format[ModelDeleted]
    implicit val fModelList: JSFormat[ModelList] = Json.format[ModelList]

    def defModelMakerRequest: JSFormat[Request] = Json.format[Request]
    def defModelMakerResponse: JSFormat[Response] = Json.format[Response]
  }

  object Request {
    implicit lazy val fModelMakerRequest: JSFormat[Request] = Formats.defModelMakerRequest
  }
  object Response {
    implicit lazy val fModelMakerResponse: JSFormat[Response] = Formats.defModelMakerResponse
  }

}

object APIModel {
  sealed trait Request
  sealed trait Response
  val service = "AModel"
  val topicRequest = "modelRequests"
  val topicResponse = "modelResponse"

  case object GetModelInfo extends Request
  case object GetModelHistory extends Request
  case object ExportModel extends Request

  case class ItemFilter(regexName: String = "", regexType: String = "")

  case class GetItem(itemID: ID) extends Request
  case class GetItems(itemIDs: List[ID]) extends Request
  case class GetItemList(from: Int = 0, size: Int = 100, filter: ItemFilter = ItemFilter("", "")) extends Request

  case object GetStructures extends Request

  case class PutItems(items: List[IDAble], info: SPAttributes = SPAttributes()) extends Request
  case class DeleteItems(items: List[ID], info: SPAttributes = SPAttributes()) extends Request

  case class UpdateModelAttributes(name: Option[String], attributes: Option[SPAttributes]) extends Request
  case class RevertModel(toVersion: Int) extends Request
  case class Import(name: String, id: ID, version: Int, attributes: SPAttributes = SPAttributes(), items: List[IDAble]) extends Request


  case class ModelToExport(name: String, id: ID, version: Int, attributes: SPAttributes = SPAttributes(), items: List[IDAble]) extends Response
  case class ModelInformation(name: String, id: ID, version: Int, noOfItems: Int, attributes: SPAttributes = SPAttributes()) extends Response
  case class ModelDeleted(model: ID) extends Response
  case class ModelUpdate(model: ID, version: Int, noOfItems: Int, updatedItems: List[IDAble] = List(), deletedItems: List[ID] = List(), info: SPAttributes = SPAttributes()) extends Response
  case class ModelHistory(model: ID, history: List[(Int, SPAttributes)]) extends Response

  case class SPItem(item: IDAble) extends Response
  case class SPItems(items: List[IDAble]) extends Response

  object Formats {
    import play.api.libs.json._

    implicit val fUpdateModelAttributes: JSFormat[UpdateModelAttributes] = Json.format[UpdateModelAttributes]
    implicit val fRevertModel: JSFormat[RevertModel] = Json.format[RevertModel]
    implicit val fImport: JSFormat[Import] = Json.format[Import]
    implicit val fExportModel : JSFormat[ExportModel.type] = deriveCaseObject[ExportModel.type]
    implicit val fGetModelInfo : JSFormat[GetModelInfo.type] = deriveCaseObject[GetModelInfo.type]
    implicit val fGetModelHistory : JSFormat[GetModelHistory.type] = deriveCaseObject[GetModelHistory.type]
    implicit val fItemFilter : JSFormat[ItemFilter] = Json.format[ItemFilter]
    implicit val fGetItem: JSFormat[GetItem] = Json.format[GetItem]
    implicit val fGetItems: JSFormat[GetItems] = Json.format[GetItems]
    implicit val fGetItemList: JSFormat[GetItemList] = Json.format[GetItemList]
    implicit val fGetStructures : JSFormat[GetStructures.type] = deriveCaseObject[GetStructures.type]
    implicit val fPutItems: JSFormat[PutItems] = Json.format[PutItems]
    implicit val fDeleteItems: JSFormat[DeleteItems] = Json.format[DeleteItems]
    implicit val fModelToExport: JSFormat[ModelToExport] = Json.format[ModelToExport]
    implicit val fModelInformation: JSFormat[ModelInformation] = Json.format[ModelInformation]
    implicit val fModelDeleted: JSFormat[ModelDeleted] = Json.format[ModelDeleted]
    implicit val fModelUpdate: JSFormat[ModelUpdate] = Json.format[ModelUpdate]
    implicit val fModelHistory: JSFormat[ModelHistory] = Json.format[ModelHistory]
    implicit val fSPItem: JSFormat[SPItem] = Json.format[SPItem]
    implicit val fSPItems: JSFormat[SPItems] = Json.format[SPItems]
    def defModelRequest: JSFormat[Request] = Json.format[Request]
    def defModelResponse: JSFormat[Response] = Json.format[Response]
  }

  object Request {
    implicit lazy val fModelRequest: JSFormat[Request] = Formats.defModelRequest
  }
  object Response {
    implicit lazy val fModelResponse: JSFormat[Response] = Formats.defModelResponse
  }

}

}
