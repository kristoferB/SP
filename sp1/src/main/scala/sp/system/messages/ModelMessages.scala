package sp.system.messages

import java.util.UUID

import sp.domain._
import sp.domain.LogicNoImplicit._
import akka.persistence._
import org.json4s.ShortTypeHints

import scala.util.Try

/**
 * Created by Kristofer on 2014-06-12.
 */

// API Inputs
sealed trait ModelAPI extends SPCommand
sealed trait ModelCommand extends ModelAPI { val model: ID }
sealed trait ModelQuery extends ModelCommand with ModelAPI
sealed trait ModelUpdate extends ModelCommand with ModelAPI

// Model messages
case class CreateModel(id: ID, name: String, attributes: SPAttributes = SPAttributes())  extends ModelAPI
// TODO Should be local in rest API. Used during json parse: KB 150526
case class CreateModelNewID(name: String, attributes: SPAttributes = SPAttributes()) //Problem to use this on the scala side. 150522 Patrik
case object GetModels extends SPCommand  with ModelAPI

case class GetIds(model: ID, ids: List[ID]) extends ModelQuery  with ModelAPI
case class GetOperations(model: ID, filter: IDAble => Boolean = _ => true) extends ModelQuery  with ModelAPI
case class GetThings(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery  with ModelAPI
case class GetSpecs(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery  with ModelAPI
case class GetResults(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery  with ModelAPI
case class GetQuery(model: ID, queryString: String = "",  filter: IDAble => Boolean = _ => true) extends ModelQuery // fix better later  with ModelAPI
case class GetDiff(model: ID, version: Long) extends ModelQuery  with ModelAPI
case class GetDiffFrom(model: ID, version: Long) extends ModelQuery  with ModelAPI
case class GetModelInfo(model: ID) extends ModelQuery  with ModelAPI

case class UpdateIDs(model: ID, items: List[IDAble], info: SPAttributes = SPAttributes()) extends ModelUpdate  with ModelAPI
case class UpdateModelInfo(model: ID, info: ModelInfo) extends ModelUpdate  with ModelAPI
case class DeleteIDs(model: ID, items: List[ID], info: SPAttributes = SPAttributes()) extends ModelUpdate  with ModelAPI
case class Revert(model: ID, toVersion: Long) extends ModelUpdate  with ModelAPI
case class DeleteModel(model: ID) extends ModelUpdate  with ModelAPI

case class ExportModel(model: ID) extends ModelCommand  with ModelAPI
case class ImportModel(model: ID, info: ModelInfo, ids: List[IDAble], history: List[ModelDiff]) extends ModelCommand  with ModelAPI


// API output

// Reply Model Messages
case class SPIDs(items: List[IDAble]) extends ModelAPI
case class ModelDiff(model: ID,
                     updatedItems: List[IDAble],
                     deletedItems: List[IDAble],
                     diffInfo: SPAttributes,
                     fromVersion: Long,
                     version: Long,
                     name: String,
                     modelAttr: SPAttributes = SPAttributes().addTimeStamp
                   ) extends SPEvent with ModelAPI
case class ModelInfos(models: List[ModelInfo]) extends ModelAPI
case class ModelInfo(id: ID, name: String, version: Long, attributes: SPAttributes, history: List[SPAttributes]) extends SPEvent with ModelAPI



