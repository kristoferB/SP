package sp.system.messages

import akka.actor.ActorRef
import sp.domain._

/**
 * Created by Kristofer on 2014-06-18.
 */

case class RegisterService(service: String,
                           ref: ActorRef,
                           attributes: SPAttributes = SPAttributes()) extends SPMessage

case object GetServices extends SPMessage
case class Services(list: Map[String, SPAttributes])

trait ServiceMessage extends SPMessage {
  val service: String
}
case class Request(service: String,
                   attributes: SPAttributes,
                   ids: List[IDAble] = List()) extends ServiceMessage

case class Response(ids: List[IDAble],
                   attributes: SPAttributes)

case class Progress(attributes: SPAttributes)