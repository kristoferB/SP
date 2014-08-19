package sp.system.messages

import java.util.UUID
import sp.domain._
import akka.persistence._

/**
 * Created by Kristofer on 2014-06-12.
 */

// API Inputs

sealed trait ModelMessage extends SPMessage { val model: String }
sealed trait ModelQuery extends ModelMessage
sealed trait ModelUpdate extends ModelMessage

// Model messages
case class CreateModel(model: String, attributes: SPAttributes = SPAttributes(Map())) extends ModelMessage
case object GetModels extends SPMessage

case class GetIds(ids: List[ID], model: String) extends ModelQuery
case class GetOperations(model: String) extends ModelQuery
case class GetThings(model: String)  extends ModelQuery
case class GetSpecs(model: String)  extends ModelQuery
case class GetStateVariable(sv: ID, model: String)  extends ModelQuery
case class GetQuery(q: SPAttributes, model: String) extends ModelQuery // fix better later
case class GetDiff(model: String, version: Long) extends ModelQuery
case class GetModelInfo(model: String) extends ModelQuery

case class UpdateIDs(model: String, items: List[UpdateID]) extends ModelUpdate
case class UpdateModelInfo(model: String, newName: String, attr: SPAttributes) extends ModelUpdate
case class UpdateID(id: ID, version: Long, item: IDAble)
object UpdateID {
  def addNew(x: IDAble) = UpdateID(x.id, 0, x)
}

// API output

// Replay Model Messages
case class SPIDs(items: List[IDAble]) extends SPMessage
case class SPSVs(svs: List[StateVariable])
case class ModelDiff(items: List[IDAble],
                     model: String,
                     fromVersion: Long,
                     currentVersion: Long,
                     attributes: SPAttributes = SPAttributes(Map("time"->DatePrimitive.now))
                   ) extends SPMessage
case class ModelInfos(models: List[ModelInfo])
case class ModelInfo(model: String, version: Long, attributes: SPAttributes)



