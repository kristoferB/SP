package sp.models

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.concurrent.duration._
import akka.persistence._
import sp.domain._
import sp.domain.LogicNoImplicit._
import sp.messages._

import scala.util.{Failure, Success, Try}


object ModelMakerAPI extends SPCommunicationAPI {
  // request
  case class CreateModel(name: String, attributes: Option[SPAttributes], model: Option[ID]) extends API
  case class DeleteModel(model: ID) extends API

  // event
  case class ModelDeleted(model: ID) extends API
  // the model itself sends out a model creation event


  sealed trait API
  sealed trait SUB
  override type MessageType = API
  override type SUBType = SUB
  override lazy val apiClasses: List[Class[_]] =   sp.macros.MacroMagic.values[MessageType]
  override lazy val apiJson: List[String] = sp.macros.MacroMagic.info[MessageType, SUBType]
}



class ModelMaker(modelActorMaker: ID => Props) extends PersistentActor with ActorLogging  {
  override def persistenceId = "modelMaker"
  import context.dispatcher

  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Subscribe }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("modelmessages", self)
  mediator ! Put(self)

  implicit val formats = ModelMakerAPI.formats

  private var modelMap: Map[ID, ActorRef] = Map()
  private var viewMap: Map[String, ActorRef] = Map()


  def receiveCommand = {
    case mess @ _ if {println(s"ModelMaker got: $mess from $sender"); false} => Unit

    case x: String =>

      val reply = sender()
      val res = ModelMakerAPI.readPF(x){messageAPI(reply)} {spMessageAPI} {reply ! _}
      if (!res) log.info("ModelMaker didn't match the string "+x)

    case x: ModelMakerAPI.API => messageAPI(sender())(x)
    case x: SPMessages => spMessageAPI(x)
    case x => log.debug("Model maker got a message it did not response to: "+x)
  }

  def messageAPI(reply: ActorRef): PartialFunction[ModelMakerAPI.API, Unit] = {
    case mess @ ModelMakerAPI.CreateModel(name, attr, idOption) =>
      val cm = mess.copy(model = idOption.orElse(Some(ID.newID)))
      if (!modelMap.contains(cm.model.get)) {
        persist(ModelMakerAPI.write(cm)) { n =>
          log.info(s"The modelHandler creates a new model called ${cm.name} id: ${cm.model.get}")
          addModel(cm)
          modelMap(cm.model.get) ! cm
          reply ! ModelMakerAPI.write(SPOK())
          log.info(s"A model was created: $cm")
        }
      } else reply ! ModelMakerAPI.write(SPError(s"Model ${cm.name} already exists"))


    case del @ ModelMakerAPI.DeleteModel(id) =>
      val reply = sender()
      if (modelMap.contains(del.model)) {
        persist(ModelMakerAPI.write(del)) { d =>
          modelMap(del.model) ! PoisonPill
          deleteModel(del)
          reply ! ModelMakerAPI.write(SPOK())
          val test = ModelMakerAPI.write(ModelMakerAPI.ModelDeleted(del.model))
          mediator ! Publish("modelevents", ModelMakerAPI.write(ModelMakerAPI.ModelDeleted(del.model)))
          log.info(s"A model was deleted: $del")
        }
      }
      else reply ! ModelMakerAPI.write(SPOK())
  }

  def spMessageAPI: PartialFunction[SPMessages, Unit] = {
    case s: StatusRequest =>
      mediator ! Publish("modelevents", ModelMakerAPI.write(sp.messages.StatusResponse(SPAttributes("service"->"ModelMaker", "noOfModels"->modelMap.keys.size, "models"->modelMap.keys.toList))))
  }


  def addModel(cm: ModelMakerAPI.CreateModel) = {
    val newModelH = context.actorOf(modelActorMaker(cm.model.get), cm.model.get.toString)
    modelMap += (cm.model.get -> newModelH)
  }

  def deleteModel(del: ModelMakerAPI.DeleteModel) = {
    if (modelMap.contains(del.model)){
      modelMap = modelMap - del.model
    }
  }

  def viewNameMaker(id: ID, v: Long) = id.toString() + " - Version: " + v

  var reMod = Map[ID, ModelMakerAPI.CreateModel]()
  def receiveRecover = {
    case x: String => ModelMakerAPI.readPF(x) {
      case cm: ModelMakerAPI.CreateModel =>
        reMod = reMod + (cm.model.get -> cm)
      case dm: ModelMakerAPI.DeleteModel =>
        reMod = reMod - dm.model
    }
    {PartialFunction.empty}
    {x => println("Recover error in modelmaker. Got command: "+x)}

    case RecoveryCompleted =>
      reMod.values.foreach(addModel)
      reMod = Map()
  }

}

object ModelMaker {
  def props(maker: ID => Props) = Props(classOf[ModelMaker], maker)
}