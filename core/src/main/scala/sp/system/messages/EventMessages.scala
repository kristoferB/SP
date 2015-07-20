package sp.system.messages

import akka.actor.ActorRef
import sp.domain.ID

object EventTargets {
  val ModelHandler: String = "ModelHandler"
  val ServiceHandler: String = "ServiceHandler"
  val RuntimeHandler: String = "RuntimeHandler"
}

object EventActions {
  val Creation: String = "Creation"
  val Deletion: String = "Deletion"
  val Update: String = "Update"
}

case class ServerSentDataEvent(data: String, event: Option[String], id: Option[String])
object ServerSentEndEvent

object ServerSentEvent {
  def apply(data: String) = ServerSentDataEvent(data, None, None)
  def apply(data: String, event: String) = ServerSentDataEvent(data, Some(event), None)
  def apply(data: String, event: String, id: String) = ServerSentDataEvent(data, Some(event), Some(id))
}

case class SubscribeToSSE(channel: ActorRef, lastEventId: Option[String])

trait EventMessage {
  val target: String
  val action: String
}

case class ModelCreated(target: String, action: String, modelInfo: ModelInfo) extends EventMessage
case class ModelDeleted(target: String, action: String, id: ID) extends EventMessage