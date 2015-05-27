package sp.system.messages

import java.util.UUID
import sp.domain._
import sp.domain.logic.AttributeLogic._
import akka.persistence._

/**
 * Created by Kristofer on 2014-06-12.
 */

// API Inputs

sealed trait ModelMessage extends SPMessage { val modelID: ID }
sealed trait ModelQuery extends ModelMessage
sealed trait ModelUpdate extends ModelMessage

// Model messages
case class CreateModel(modelID: ID = ID.newID, name: String, attributes: SPAttributes = SPAttributes()) extends ModelMessage
// TODO Should be local in rest API. Used during json parse: KB 150526
case class CreateModelNewID(name: String, attributes: SPAttributes = SPAttributes()) //Problem to use this on the scala side. 150522 Patrik
case object GetModels extends SPMessage

case class GetIds(modelID: ID, ids: List[ID]) extends ModelQuery
case class GetOperations(modelID: ID, filter: IDAble => Boolean = _ => true) extends ModelQuery
case class GetThings(modelID: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery
case class GetSpecs(modelID: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery
case class GetResults(modelID: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery
case class GetQuery(modelID: ID, queryString: String = "",  filter: IDAble => Boolean = _ => true) extends ModelQuery // fix better later
case class GetDiff(modelID: ID, version: Long) extends ModelQuery
case class GetDiffFrom(modelID: ID, version: Long) extends ModelQuery
case class GetModelInfo(modelID: ID) extends ModelQuery

case class UpdateIDs(modelID: ID, modelVersion: Long, items: List[IDAble]) extends ModelUpdate
case class UpdateModelInfo(modelID: ID, info: ModelInfo) extends ModelUpdate
case class DeleteIDs(modelID: ID, items: List[ID]) extends ModelUpdate
case class Revert(modelID: ID, toVersion: Long) extends ModelUpdate


// API output

// Replay Model Messages
case class SPIDs(items: List[IDAble]) extends SPMessage
case class ModelDiff(modelID: ID,
                     updatedItems: List[IDAble],
                     deletedItems: List[IDAble],
                     fromVersion: Long,
                     currentVersion: Long,
                     name: String,
                     attributes: SPAttributes = SPAttributes().addTimeStamp
                   ) extends SPMessage
case class ModelInfos(models: List[ModelInfo])
case class ModelInfo(modelID: ID, name: String, version: Long, attributes: SPAttributes)




