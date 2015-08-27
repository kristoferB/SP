package sp.system.messages

import akka.actor.ActorRef
import sp.domain.ID

object eventTargets {
  val modelService: String = "modelService"
  val itemService: String = "itemService"
  val serviceService: String = "serviceService"
  val runtimeService: String = "runtimeService"
}

object eventTypes {
  val creation: String = "creation"
  val deletion: String = "deletion"
  val update: String = "update"
}

case class ServerSentDataEvent(data: String, event: Option[String], id: Option[String])
object serverSentEndEvent

object serverSentEvent {
  def apply(data: String) = ServerSentDataEvent(data, None, None)
  def apply(data: String, event: String) = ServerSentDataEvent(data, Some(event), None)
  def apply(data: String, event: String, id: String) = ServerSentDataEvent(data, Some(event), Some(id))
}

case class SubscribeToSSE(channel: ActorRef, lastEventId: Option[String])

trait EventMessage {
  val target: String
  val event: String
}

case class ModelCreated(target: String, event: String, modelInfo: ModelInfo) extends EventMessage
case class ModelDeleted(target: String, event: String, id: ID) extends EventMessage
case class ModelInfoUpdated(target: String, event: String, modelInfo: ModelInfo) extends EventMessage

case class ItemsUpdated(target: String, event: String, updated: SPIDs) extends EventMessage
case class ItemDeleted(target: String, event: String, deleted: SPIDs) extends EventMessage