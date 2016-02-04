package sp.system.messages

import java.util.UUID
import sp.domain._
import sp.domain.logic.AttributeLogic._
import akka.persistence._

/**
 * Created by Kristofer on 2014-06-12.
 */

// API Inputs
sealed trait ModelCommand extends SPCommand { val model: ID }
sealed trait ModelQuery extends ModelCommand
sealed trait ModelUpdate extends ModelCommand

// Model messages
case class CreateModel(id: ID, name: String, attributes: SPAttributes = SPAttributes())
// TODO Should be local in rest API. Used during json parse: KB 150526
case class CreateModelNewID(name: String, attributes: SPAttributes = SPAttributes()) //Problem to use this on the scala side. 150522 Patrik
case object GetModels extends SPCommand

case class GetIds(model: ID, ids: List[ID]) extends ModelQuery
case class GetOperations(model: ID, filter: IDAble => Boolean = _ => true) extends ModelQuery
case class GetThings(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery
case class GetSpecs(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery
case class GetResults(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery
case class GetQuery(model: ID, queryString: String = "",  filter: IDAble => Boolean = _ => true) extends ModelQuery // fix better later
case class GetDiff(model: ID, version: Long) extends ModelQuery
case class GetDiffFrom(model: ID, version: Long) extends ModelQuery
case class GetModelInfo(model: ID) extends ModelQuery

case class UpdateIDs(model: ID, items: List[IDAble], info: SPAttributes = SPAttributes()) extends ModelUpdate
case class UpdateModelInfo(model: ID, info: ModelInfo) extends ModelUpdate
case class DeleteIDs(model: ID, items: List[ID], info: SPAttributes = SPAttributes()) extends ModelUpdate
case class Revert(model: ID, toVersion: Long) extends ModelUpdate
case class DeleteModel(model: ID) extends ModelUpdate

case class ExportModel(model: ID) extends ModelCommand
case class ImportModel(model: ID, info: ModelInfo, ids: List[IDAble], history: List[ModelDiff]) extends ModelCommand


// API output

// Replay Model Messages
case class SPIDs(items: List[IDAble])
case class ModelDiff(model: ID,
                     updatedItems: List[IDAble],
                     deletedItems: List[IDAble],
                     diffInfo: SPAttributes,
                     fromVersion: Long,
                     version: Long,
                     name: String,
                     modelAttr: SPAttributes = SPAttributes().addTimeStamp
                   ) extends SPEvent
case class ModelInfos(models: List[ModelInfo])
case class ModelInfo(id: ID, name: String, version: Long, attributes: SPAttributes, history: List[SPAttributes]) extends SPEvent




