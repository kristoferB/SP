package sp.system.messages

import akka.actor.ActorRef
import sp.domain._

/**
 * Created by Kristofer on 2014-06-18.
 */

case class RegisterService(service: String,
                           ref: ActorRef,
                           attributes: SPAttributes = SPAttributes()) extends SPCommand

case class RemoveService(service: String) extends ServiceCommand
case object GetServices extends SPCommand
case class ServiceInfo(name: String, attributes: SPAttributes)
case class ServiceInfos(services: List[ServiceInfo])

trait ServiceCommand extends SPCommand {
  val service: String
}

/**
 * To request a response from the service. The service will response with a Response message,
 * a Progress Message or a SPError
 *
 * There are a number of predefined attributes:
 * val model: ID // The model id
 * val toModel: Boolean // if the result from the service should update the model
 * val toBus: Boolean // if the result from the service should be forwarded out on the message bus
 * val onlyResponse: Boolean // replies to the asker only when done (used when asking with ? )
 * val fillIDs: List[ID] // if exists and ids is empty, the items with these ids will be added
 *                     // to the request.ids instead of the complete model
 *
 * @param service The name of the service
 * @param attributes The input to the service.
 * @param ids a list of items that the service should work with. If empty, all items from
 *            the model will be included if the model is defined in the attributes
 */
case class Request(service: String,
                   attributes: SPAttributes,
                   ids: List[IDAble] = List(),
                   reqID: ID = ID.newID) extends ServiceCommand

/**
 * The final answer from the service
 * @param ids A list of items that have been created or updated by the service
 * @param attributes Information about the result
 */
case class Response(ids: List[IDAble],
                    attributes: SPAttributes,
                    service: String,
                    reqID: ID) extends SPEvent

/**
 * If the service takes longer time to answer, it should return these messages
 * @param attributes
 */
case class Progress(attributes: SPAttributes, service: String, reqID: ID) extends SPEvent

/**
 * Used by an service for defining its input for a UI.
 * @param ofType
 * @param domain
 * @param default
 */
case class KeyDefinition(ofType: String, domain: List[SPValue] = List(), default: Option[SPValue] = None)
