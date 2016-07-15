package sp.models

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.concurrent.duration._
import akka.persistence._
import sp.domain._
import sp.domain.Logic._
import sp.messages._

import scala.util.{Failure, Success, Try}



// API
sealed trait ModelMakerMessages

// request
case class CreateModel(name: String, attributes: Option[SPAttributes], model: Option[ID]) extends ModelMakerMessages
case class DeleteModel(model: ID) extends ModelMakerMessages

// event
case class ModelDeleted(model: ID) extends ModelMakerMessages
// the model itself sends out a model creation event



object ModelMakerAPI extends SPCommunicationAPI {
  override type MessageType = ModelMakerMessages
  override val apiFormats = List(
    classOf[CreateModel],
    classOf[DeleteModel],
    classOf[ModelDeleted]
  )


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
    //case mess @ _ if {println(s"ModelMaker got: $mess from $sender"); false} => Unit

    case x: String =>
      val reply = sender()
      val res = ModelMakerAPI.readPF(x){messageAPI(reply)} {spMessageAPI} {reply ! _}
      if (!res) log.info("ModelMaker didn't match the string "+x)

    case x: ModelMakerMessages => messageAPI(sender())(x)
    case x: SPMessages => spMessageAPI(x)
    case x => log.debug("Model maker got a message it did not response to: "+x)
  }

  def messageAPI(reply: ActorRef): PartialFunction[ModelMakerMessages, Unit] = {
    case mess @ sp.models.CreateModel(name, attr, idOption) =>
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


    case del @ sp.models.DeleteModel(id) =>
      val reply = sender()
      if (modelMap.contains(del.model)) {
        persist(ModelMakerAPI.write(del)) { d =>
          modelMap(del.model) ! PoisonPill
          deleteModel(del)
          reply ! ModelMakerAPI.write(SPOK())
          val test = ModelMakerAPI.write(ModelDeleted(del.model))
          mediator ! Publish("modelevents", ModelMakerAPI.write(ModelDeleted(del.model)))
          log.info(s"A model was deleted: $del")
        }
      }
      else reply ! ModelMakerAPI.write(SPOK())
  }

  def spMessageAPI: PartialFunction[SPMessages, Unit] = {
    case s: StatusRequest =>
      mediator ! Publish("modelevents", ModelMakerAPI.write(sp.messages.StatusResponse(SPAttributes("service"->"ModelMaker", "noOfModels"->modelMap.keys.size, "models"->modelMap.keys.toList))))
  }


  def addModel(cm: CreateModel) = {
    val newModelH = context.actorOf(modelActorMaker(cm.model.get))
    modelMap += (cm.model.get -> newModelH)
  }

  def deleteModel(del: DeleteModel) = {
    if (modelMap.contains(del.model)){
      modelMap = modelMap - del.model
    }
  }

  def viewNameMaker(id: ID, v: Long) = id.toString() + " - Version: " + v

  var reMod = Map[ID, CreateModel]()
  def receiveRecover = {
    case x: String => ModelMakerAPI.readPF(x) {
      case cm: CreateModel =>
        reMod = reMod + (cm.model.get -> cm)
      case dm: DeleteModel =>
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