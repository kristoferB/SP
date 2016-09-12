package sp.system.messages

import sp.domain._
import java.util.UUID

import akka.actor.ActorRef
import sp.domain._
import sp.domain.LogicNoImplicit._
import akka.persistence._
import org.json4s.ShortTypeHints

import scala.util.Try

sealed trait SPMessage
sealed trait SPCommand extends SPMessage
sealed trait SPEvent extends SPMessage


case class SPOK(isa: String = "SPOK") extends SPEvent


// Error messages
trait SPError extends SPEvent
object SPError {
  def apply(s: String): SPError = SPErrorString(s)
  def apply(xs: List[SPError]): SPError = SPErrors(xs)
}
case class SPErrorString(error: String) extends SPError
case class SPErrors(errors: List[SPError]) extends SPError
case class UpdateError(currentModelVersion: Long, conflicts: List[ID]) extends SPError
case class MissingID(id: ID, error: String = s"The Model does not contain that id") extends SPError
case class ServiceError(service: String, reqID: ID, serviceError: SPError) extends SPError


// API Inputs
sealed trait ModelAPI extends SPCommand
sealed trait ModelCommand extends ModelAPI { val model: ID }
sealed trait ModelQuery extends ModelCommand with ModelAPI
sealed trait ModelUpdate extends ModelCommand with ModelAPI

// Model messages
case class CreateModel(id: ID, name: String, attributes: SPAttributes = SPAttributes())  extends ModelAPI
// TODO Should be local in rest API. Used during json parse: KB 150526
case class CreateModelNewID(name: String, attributes: SPAttributes = SPAttributes()) //Problem to use this on the scala side. 150522 Patrik
case object GetModels extends SPCommand  with ModelAPI

case class GetIds(model: ID, ids: List[ID]) extends ModelQuery  with ModelAPI
case class GetOperations(model: ID, filter: IDAble => Boolean = _ => true) extends ModelQuery  with ModelAPI
case class GetThings(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery  with ModelAPI
case class GetSpecs(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery  with ModelAPI
case class GetResults(model: ID, filter: IDAble => Boolean = _ => true)  extends ModelQuery  with ModelAPI
case class GetQuery(model: ID, queryString: String = "",  filter: IDAble => Boolean = _ => true) extends ModelQuery // fix better later  with ModelAPI
case class GetDiff(model: ID, version: Long) extends ModelQuery  with ModelAPI
case class GetDiffFrom(model: ID, version: Long) extends ModelQuery  with ModelAPI
case class GetModelInfo(model: ID) extends ModelQuery  with ModelAPI

case class UpdateIDs(model: ID, items: List[IDAble], info: SPAttributes = SPAttributes()) extends ModelUpdate  with ModelAPI
case class UpdateModelInfo(model: ID, info: ModelInfo) extends ModelUpdate  with ModelAPI
case class DeleteIDs(model: ID, items: List[ID], info: SPAttributes = SPAttributes()) extends ModelUpdate  with ModelAPI
case class Revert(model: ID, toVersion: Long) extends ModelUpdate  with ModelAPI
case class DeleteModel(model: ID) extends ModelUpdate  with ModelAPI

case class ExportModel(model: ID) extends ModelCommand  with ModelAPI
case class ImportModel(model: ID, info: ModelInfo, ids: List[IDAble], history: List[ModelDiff]) extends ModelCommand  with ModelAPI


// API output

// Reply Model Messages
case class SPIDs(items: List[IDAble]) extends ModelAPI
case class ModelDiff(model: ID,
                     updatedItems: List[IDAble],
                     deletedItems: List[IDAble],
                     diffInfo: SPAttributes,
                     fromVersion: Long,
                     version: Long,
                     name: String,
                     modelAttr: SPAttributes = SPAttributes().addTimeStamp
                    ) extends SPEvent with ModelAPI
case class ModelInfos(models: List[ModelInfo]) extends ModelAPI
case class ModelInfo(id: ID, name: String, version: Long, attributes: SPAttributes, history: List[SPAttributes]) extends SPEvent with ModelAPI



sealed trait ModelEvent extends SPEvent
case class ModelAdded(id: ID, attributes: SPAttributes) extends ModelEvent
case class ModelDeleted(id: ID,  attributes: SPAttributes) extends ModelEvent
case class ModelUpdated(id: ID,  attributes: SPAttributes) extends ModelEvent







sealed trait ServiceAPI extends SPCommand

case class RegisterService(service: String,
                           ref: ActorRef,
                           attributes: SPAttributes = SPAttributes(),
                           validate: List[TransformValue[_]] = List()) extends ServiceAPI

case class RemoveService(service: String) extends ServiceCommand with SPEvent
case object GetServices extends ServiceAPI
case class ServiceInfo(name: String, attributes: SPAttributes) extends SPEvent
case class ServiceInfos(services: List[ServiceInfo])
case class ServiceHandlerAttributes(model: Option[ID],
                                    responseToModel: Boolean,
                                    onlyResponse: Boolean,
                                    includeIDAbles: List[ID])

trait ServiceCommand extends ServiceAPI {
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
                    reqID: ID) extends SPEvent with ServiceAPI

/**
  * If the service takes longer time to answer, it should return these messages
  * @param attributes
  */
case class Progress(attributes: SPAttributes, service: String, reqID: ID) extends SPEvent with ServiceAPI

/**
  * Used by an service for defining its input for a UI.
  * @param ofType The scala type name that is expected, like "String", "Int", "Boolean", "Option[ID]"...
  * @param domain Include a list of possible values. If empty, any value is possible
  * @param default Include a default value if the key is missing. If None (and the ofType is not an Option)
  *                the service will return an error
  */
case class KeyDefinition(ofType: String, domain: List[SPValue] = List(), default: Option[SPValue] = None)



/**
  * Used for validating the request
  * @param transform
  * @tparam T
  */
case class TransformValue[T](key: String, transform: SPAttributes => Option[T])





// Service Events
case class ServiceEvent(sender: ID, attributes: SPAttributes) extends SPEvent

case class SubscribeToSSE(channel: ActorRef)
case class UnSubscribeToSSE(channel: ActorRef)