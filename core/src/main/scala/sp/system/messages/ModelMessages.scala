package sp.system.messages

import java.util.UUID
import sp.domain._
import akka.persistence._

/**
 * Created by Kristofer on 2014-06-12.
 */

// API Inputs

sealed trait ModelMessage { val model: String }
sealed trait ModelQuery extends ModelMessage
sealed trait ModelUpdate extends ModelMessage

// Model messages
case class CreateModel(model: String) extends ModelMessage
case object GetModels extends SPMessage

case class GetIds(ids: List[ID], model: String) extends ModelQuery
case class GetOperations(model: String) extends ModelQuery
case class GetThings(model: String)  extends ModelQuery
case class GetSpecs(model: String)  extends ModelQuery
case class GetQuery(q: SPAttributes, model: String) extends ModelQuery // fix better later
case class GetDiff(model: String, version: Long) extends ModelQuery

case class UpdateIDs(model: String, version: Long, ids: List[UpdateID]) extends ModelUpdate
case class UpdateID(id: ID, version: Long, updated: IDAble)
object UpdateID {
  def addNew(x: IDAble) = UpdateID(x.id, x.version, x)
}

// API output

// Replay Model Messages
case class SPIDs(model: String, version: Long, ids: List[IDAble]) extends SPMessage
case class ModelDiff(ids: List[IDAble],
                     model: String,
                     prevVersion: Long,
                     version: Long,
                     time: DatePrimitive = DatePrimitive.now) extends SPMessage
case class ModelInfo(model: String, version: Long)



