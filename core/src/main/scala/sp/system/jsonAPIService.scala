package sp.system

import akka.actor.Actor.Receive
import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.SubscribeAck
import akka.pattern.ask
import akka.util.Timeout
import akka.cluster.pubsub._
import org.json4s.ShortTypeHints

import scala.concurrent.duration._
import sp.system.messages._
import sp.domain.LogicNoImplicit._
import sp.messages.SPCommunicationAPI

import scala.util.Try

/**
  * Created by kristofer on 2016-08-25.
  *
  * Translates messages in the old structure to the json version. Used during migration
  *
  */
class OldModelAPIService extends Actor with OldModelMessageAPITransformer {
  implicit val timeout = Timeout(1 seconds)
  import context.dispatcher

  import DistributedPubSubMediator.{ Put, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("models", self)

  implicit val f = OldModelMessageAPI.formats

  def receive = {
    case x if sender() == self => // Do not read my messages
    case x if {if (!x.isInstanceOf[SubscribeAck]) println("jsonAPI got: "+x);false} => false

    case x: String if OldModelMessageAPI.read(x).isSuccess =>
      val reply = sender()
      println("Match a model message!")
      OldModelMessageAPI.read(x).map{ mess =>
          mediator ? Publish("modelHandler", convertToOld(mess)) map{r =>
            println("GOT as REPLY: " + r)
            reply ! convertToJson(r)
          }
      }
  }

}

class JsonAPIEvents extends Actor {
  import DistributedPubSubMediator.{ Put, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator

  // Add all topics here
  mediator ! Subscribe("eventHandler", self)
  mediator ! Subscribe("events", self)

  val api = new OldAPI {}

  def receive = {
    case x if sender() == self =>
    case x if {if (!x.isInstanceOf[SubscribeAck]) println("jsonAPI events got:");false} => false

    case x: String if api.readModels(x).isSuccess =>
      api.readModels(x).map{ mess =>
        mediator ! Publish("eventHandler", mess)
      }

    case x: String if api.readServices(x).isSuccess =>
      api.readServices(x).map{ mess =>
        mediator ! Publish("eventHandler", mess)
      }

    case x: String if api.readEvents(x).isSuccess =>
      api.readEvents(x).map{ mess =>
        mediator ! Publish("eventHandler", mess)
      }

    case x: String =>
      println("got a string:")
      println(x)
      println("")

    case x: ModelAPI => mediator ! Publish("models", api.write(x))
    case x: ServiceAPI => mediator ! Publish("services", api.write(x))
    case x: SPEvent => mediator ! Publish("services", api.write(x))

    case x if !x.isInstanceOf[SubscribeAck]  =>
      println("got a message as class:")
      println(x)
      println("")

  }
}

object OldModelAPIService {
  def props = Props(classOf[OldModelAPIService])
}

object JsonAPIEvents {
  def props = Props(classOf[JsonAPIEvents])
}

// Temporary solution until we have moved to the new model under sp.model
trait OldAPI {
  val spFormats = new JsonFormats{}

  def write[T <: AnyRef](x: T) = {
    val temp = sp.macros.MacroMagic.values[ModelAPI] ++ sp.macros.MacroMagic.values[ServiceAPI]
    implicit val f = new JsonFormats {
      override val typeHints = ShortTypeHints(spFormats.typeHints.hints ++ temp)
    }
    org.json4s.native.Serialization.write[T](x)
  }
  def readModels(x: String) = {
    val temp = sp.macros.MacroMagic.values[ModelAPI] ++ sp.macros.MacroMagic.values[ServiceAPI]
    implicit val f = new JsonFormats {
      override val typeHints = ShortTypeHints(spFormats.typeHints.hints ++ temp)
    }
    Try(org.json4s.native.Serialization.read[ModelAPI](x))
  }
  def readServices(x: String) = {
    val temp = sp.macros.MacroMagic.values[ModelAPI] ++ sp.macros.MacroMagic.values[ServiceAPI]
    implicit val f = new JsonFormats {
      override val typeHints = ShortTypeHints(spFormats.typeHints.hints ++ temp)
    }
    Try(org.json4s.native.Serialization.read[ServiceAPI](x))
  }
  def readEvents(x: String) = {
    val temp = sp.macros.MacroMagic.values[ModelAPI] ++ sp.macros.MacroMagic.values[ServiceAPI]
    implicit val f = new JsonFormats {
      override val typeHints = ShortTypeHints(spFormats.typeHints.hints ++ temp)
    }

    Try(org.json4s.native.Serialization.read[SPErrorString](x)) orElse
      Try(org.json4s.native.Serialization.read[SPErrors](x)) orElse
      Try(org.json4s.native.Serialization.read[UpdateError](x)) orElse
      Try(org.json4s.native.Serialization.read[MissingID](x)) orElse
      Try(org.json4s.native.Serialization.read[ServiceError](x)) orElse
      Try(org.json4s.native.Serialization.read[SPOK](x))
  }
}

trait OldModelMessageAPITransformer {
  def convertToJson(ev: Any): String  = {
    implicit val f = OldModelMessageAPI.formats
    ev match {
      case x: SPOK => OldModelMessageAPI.write(OldModelMessageAPI.SPOK())
      case x: CreateModel => OldModelMessageAPI.write(OldModelMessageAPI.CreateModel(x.id, x.name, x.attributes))
      case x: ModelInfo => OldModelMessageAPI.write(OldModelMessageAPI.ModelInfo(x.id, x.name, x.version, x.attributes, x.history))
      case x: ModelDiff => OldModelMessageAPI.write(OldModelMessageAPI.ModelDiff(x.model, x.updatedItems, x.deletedItems, x.diffInfo, x.fromVersion, x.version, x.name, x.modelAttr))
    }
  }
  def convertToOld(mess: OldModelMessageAPI.API): SPMessage = {
    mess match {
      case x: OldModelMessageAPI.CreateModel => CreateModel(x.id, x.name, x.attributes)
    }
  }
}

import sp.domain._
object OldModelMessageAPI extends SPCommunicationAPI {

  // Commands
  case class CreateModel(id: ID, name: String, attributes: SPAttributes = SPAttributes())  extends API
//  case class GetModels() extends API

//  case class GetIds(model: ID, ids: List[ID]) extends API
//  case class GetDiff(model: ID, version: Long) extends API
//  case class GetDiffFrom(model: ID, version: Long) extends API
//  case class GetModelInfo(model: ID) extends API
//
//  case class UpdateIDs(model: ID, items: List[IDAble], info: SPAttributes = SPAttributes()) extends API
//  case class UpdateModelInfo(model: ID, info: ModelInfo) extends API
//  case class DeleteIDs(model: ID, items: List[ID], info: SPAttributes = SPAttributes()) extends API
//  case class Revert(model: ID, toVersion: Long) extends API
//  case class DeleteModel(model: ID) extends API
//
//  case class ExportModel(model: ID) extends API
//  case class ImportModel(model: ID, info: ModelInfo, ids: List[IDAble], history: List[ModelDiff]) extends API

  // Replies
  case class SPOK() extends API
  case class SPIDs(items: List[IDAble]) extends API
  case class ModelInfo(id: ID, name: String, version: Long, attributes: SPAttributes, history: List[SPAttributes]) extends API
  case class ModelDiff(model: ID,
                       updatedItems: List[IDAble],
                       deletedItems: List[IDAble],
                       diffInfo: SPAttributes,
                       fromVersion: Long,
                       version: Long,
                       name: String,
                       modelAttr: SPAttributes = SPAttributes().addTimeStamp
                      ) extends API


  sealed trait API
  sealed trait SUB
  override type MessageType = API
  override type SUBType = SUB
  override lazy val apiClasses: List[Class[_]] =   sp.macros.MacroMagic.values[MessageType]
  override lazy val apiJson: List[String] = sp.macros.MacroMagic.info[MessageType, SUBType]


}






//class ModelHandlerJson(mh: ActorRef) extends Actor {
//  implicit val timeout = Timeout(1 seconds)
//  import context.dispatcher
//  implicit val f = api.formats
//
//  def receive = {
//    case x: String =>
//      val r = sender()
//      val conv = api.read(x)
//      conv.foreach(mess =>
//        (mh ? mess).map{
//          case res: ModelAPI => r ! OldModelAPI.write(res)
//          case res => r ! res
//        }
//      )
//      println(s"ModelHandler json got string: $x and converted it to "+conv)
//    case x: ModelAPI =>
//      println("modelHandlerJson got: "+x)
//
//      import DistributedPubSubMediator.{ Put, Subscribe, Publish }
//      val mediator = DistributedPubSub(context.system).mediator
//      mediator ! Publish("modelResponse", OldModelAPI.write(x))
//
//  }
//
//}
//
//object ModelHandlerJson {
//  def props(mh: ActorRef) = Props(classOf[ModelHandlerJson], mh)
//}