package sp.models

import sp.domain._
import sp.messages._
import Pickles._
import scala.util.{Try}


package APIModels {
  sealed trait Request
  case class CreateModel(name: String, attributes: Option[SPAttributes], id: ID) extends Request
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

  sealed trait Response
  case class TheModel(name: String, id: ID, version: Int, attributes: SPAttributes = SPAttributes(), items: List[IDAble]) extends Response
  case class ModelInformation(name: String, id: ID, version: Int, attributes: SPAttributes = SPAttributes()) extends Response
  case class ModelDeleted(model: ID) extends Response
  case class ModelUpdate(model: ID, version: Int, updatedItems: List[IDAble] = List(), deletedItems: List[ID] = List(), info: SPAttributes = SPAttributes()) extends Response
  case class ModelHistory(model: ID, history: List[(Int, SPAttributes)]) extends Response
  case class ModelList(models: List[ID]) extends Response


  case class SPItem(item: IDAble) extends Response
  case class SPItems(items: List[IDAble]) extends Response


  object attributes {
    val service = "Models"
  }
}

import sp.models.{APIModels => api}


object ModelsComm {
  def extractRequest(mess: Try[SPMessage], id: String) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] if h.to == id || h.to == api.attributes.service
    b <- m.getBodyAs[api.Request]
  } yield (h, b)

  def extractAPISP(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP]
  } yield (h, b)


  def makeMess(h: SPHeader, b: api.Response) = SPMessage.makeJson[SPHeader, api.Response](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)
}
