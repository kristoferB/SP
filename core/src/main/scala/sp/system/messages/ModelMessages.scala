package sp.system.messages

import java.util.UUID
import sp.domain._
import akka.persistence._

/**
 * Created by Kristofer on 2014-06-12.
 */

// API Inputs

sealed trait ModelMessage extends SPMessage { val model: ID }
sealed trait ModelQuery extends ModelMessage
sealed trait ModelUpdate extends ModelMessage

// Model messages
case class CreateModel(model: ID, name: String, attributes: SPAttributes = SPAttributes(Map())) extends ModelMessage
case class CreateModelNewID(name: String, attributes: SPAttributes = SPAttributes(Map()))
case object GetModels extends SPMessage

case class GetIds(model: ID, ids: List[ID]) extends ModelQuery
case class GetOperations(model: ID, filter: IDAble => Boolean = _ => true) extends ModelQuery
case class GetThings(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery
case class GetSpecs(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery
case class GetResults(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery
case class GetQuery(model: ID, queryString: String = "",  filter: IDAble => Boolean = _ => true) extends ModelQuery // fix better later
case class GetDiff(model: ID, version: Long) extends ModelQuery
case class GetDiffFrom(model: ID, version: Long) extends ModelQuery
case class GetModelInfo(model: ID) extends ModelQuery

case class UpdateIDs(model: ID, modelVersion: Long, items: List[IDAble]) extends ModelUpdate
case class DeleteIDs(model: ID, items: List[ID]) extends ModelUpdate

case class Revert(model: ID, toVersion: Long) extends ModelUpdate

// API output

// Replay Model Messages
case class SPIDs(items: List[IDAble]) extends SPMessage
case class ModelDiff(model: ID,
                     updatedItems: List[IDAble],
                     deletedItems: List[IDAble],
                     fromVersion: Long,
                     currentVersion: Long,
                     name: String,
                     attributes: SPAttributes = SPAttributes(Map("time"->DatePrimitive.now))
                   ) extends SPMessage
case class ModelInfos(models: List[ModelInfo])
case class ModelInfo(model: ID, name: String, version: Long, attributes: SPAttributes)




