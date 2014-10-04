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
case class GetStateVariable(model: ID, sv: ID)  extends ModelQuery
case class GetStateVariables(model: ID, filter: StateVariable => Boolean = _ => true)  extends ModelQuery
case class GetQuery(model: ID, queryString: String = "",  filter: IDAble => Boolean = _ => true) extends ModelQuery // fix better later
case class GetDiff(model: ID, version: Long) extends ModelQuery
case class GetDiffFrom(model: ID, version: Long) extends ModelQuery
case class GetModelInfo(model: ID) extends ModelQuery

case class UpdateIDs(model: ID, items: List[UpdateID]) extends ModelUpdate
case class DeleteIDs(model: ID, items: List[ID]) extends ModelUpdate
case class UpdateModelInfo(model: ID, newName: String, attr: SPAttributes) extends ModelUpdate
case class UpdateID(id: ID, version: Long, item: IDAble)
object UpdateID {
  def addNew(x: IDAble) = UpdateID(x.id, 0, x)
}

// API output

// Replay Model Messages
case class SPIDs(items: List[IDAble]) extends SPMessage
case class SPSVs(svs: List[StateVariable])
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

// View Messages
case class CreateView(model: ID, version: Long) extends SPMessage



