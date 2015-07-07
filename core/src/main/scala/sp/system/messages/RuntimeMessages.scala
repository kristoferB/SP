package sp.system.messages

import akka.actor.{ActorRef, Props}
import sp.domain._


// Send
case class RegisterRuntimeKind(name: String, props: RuntimeInfo => Props, attributes: SPAttributes)
case object GetRuntimeKinds

// receive
case class RuntimeKindInfo(name: String, attributes: SPAttributes)
case class RuntimeKindInfos(runtimes: List[RuntimeKindInfo])

// send
case class CreateRuntime(kind: String, model: ID, name: String, settings: Option[SPAttributes])

case object GetRuntimes
case class StopRuntime(runtime: ID)

// receive
case class RuntimeInfo(id: ID, kind: String, model: ID, name: String, settings: Option[SPAttributes])
case class RuntimeInfos(runtimes: List[RuntimeInfo])

// Messages to talk to various runtimes
trait RuntimeMessage {
  val runtime: ID
}

case class SubscribeToSSE(runtime: ID, channel: ActorRef, lastEventId: Option[String]) extends RuntimeMessage
case class SimpleMessage(runtime: ID, attributes: SPAttributes) extends RuntimeMessage

