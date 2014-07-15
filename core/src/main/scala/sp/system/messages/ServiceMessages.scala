package sp.system.messages

import akka.actor.ActorRef
import sp.domain.SPAttributes

/**
 * Created by Kristofer on 2014-06-18.
 */

case class RegisterService(service: String, ref: ActorRef) extends SPMessage
case object GetServices extends SPMessage

trait ServiceMessage extends SPMessage {
  val service: String
}
case class Request(service: String, attributes: SPAttributes) extends ServiceMessage