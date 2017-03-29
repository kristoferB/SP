package sp.models

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.concurrent.duration._
import akka.persistence._
import sp.domain._
import sp.domain.LogicNoImplicit._
import sp.messages.Pickles.SPMessage
import sp.messages._

import scala.util.{Failure, Success, Try}


import sp.models.{APIModels => api}


class ModelMaker(modelActorMaker: api.CreateModel => Props) extends PersistentActor with ActorLogging  {
  override def persistenceId = "modelMaker"
  import context.dispatcher
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Subscribe }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Put(self)

  private var modelMap: Map[ID, ActorRef] = Map()

  val instanceID = ID.newID.toString


  def receiveCommand = {
    case mess @ _ if {println(s"ModelMaker got: $mess from $sender"); false} => Unit

    case x: String if sender() != self =>
      val mess = SPMessage.fromJson(x)

      ModelsComm.extractRequest(mess, instanceID).toOption.collect{
        case (h, b: api.CreateModel) =>
          val updH = h.copy(from = api.attributes.service, to = h.from)
          if (modelMap.contains(b.id)){
            mediator ! Publish("anwers", ModelsComm.makeMess(updH, APISP.SPError(s"Model ${b.id} already exist. Can not be created")))
          } else {
            mediator ! Publish("anwers", ModelsComm.makeMess(updH, APISP.SPACK()))
            persist(x){mess =>
              createModel(b)
              mediator ! Publish("anwers", ModelsComm.makeMess(updH, APISP.SPDone()))
            }

          }
        case (h, b: api.DeleteModel) =>
          // the model replies and terminates
          persist(x){mess =>
            deleteModel(b)
          }
        case (h, b: api.GetModels) =>
          val mess = ModelsComm.makeMess(h.copy(from = api.attributes.service, to = h.from), api.ModelList(modelMap.keys.toList))
          mediator ! Publish("answers", mess)
      }

  }

  def createModel(model: api.CreateModel) = {
    val a = context.actorOf(modelActorMaker(model))
    modelMap += (model.id -> a)
  }

    def deleteModel(del: api.DeleteModel) = {
      if (modelMap.contains(del.id)){
        modelMap = modelMap - del.id
      }
    }

//  def messageAPI(reply: ActorRef): PartialFunction[ModelMakerAPI.API, Unit] = {
//    case mess @ ModelMakerAPI.CreateModel(name, attr, idOption) =>
//      val cm = mess.copy(model = idOption.orElse(Some(ID.newID)))
//      if (!modelMap.contains(cm.model.get)) {
//        persist(ModelMakerAPI.write(cm)) { n =>
//          log.info(s"The modelHandler creates a new model called ${cm.name} id: ${cm.model.get}")
//          addModel(cm)
//          modelMap(cm.model.get) ! cm
//          reply ! ModelMakerAPI.write(SPOK())
//          log.info(s"A model was created: $cm")
//        }
//      } else reply ! ModelMakerAPI.write(SPError(s"Model ${cm.name} already exists"))
//
//
//    case del @ ModelMakerAPI.DeleteModel(id) =>
//      val reply = sender()
//      if (modelMap.contains(del.model)) {
//        persist(ModelMakerAPI.write(del)) { d =>
//          modelMap(del.model) ! PoisonPill
//          deleteModel(del)
//          reply ! ModelMakerAPI.write(SPOK())
//          val test = ModelMakerAPI.write(ModelMakerAPI.ModelDeleted(del.model))
//          mediator ! Publish("modelevents", ModelMakerAPI.write(ModelMakerAPI.ModelDeleted(del.model)))
//          log.info(s"A model was deleted: $del")
//        }
//      }
//      else reply ! ModelMakerAPI.write(SPOK())
//  }
//
//  def spMessageAPI: PartialFunction[SPMessages, Unit] = {
//    case s: StatusRequest =>
//      mediator ! Publish("modelevents", ModelMakerAPI.write(sp.messages.StatusResponse(SPAttributes("service"->"ModelMaker", "noOfModels"->modelMap.keys.size, "models"->modelMap.keys.toList))))
//  }


//  def addModel(cm: ModelMakerAPI.CreateModel) = {
//    val newModelH = context.actorOf(modelActorMaker(cm.model.get), cm.model.get.toString)
//    modelMap += (cm.model.get -> newModelH)
//  }
//
//  def deleteModel(del: ModelMakerAPI.DeleteModel) = {
//    if (modelMap.contains(del.model)){
//      modelMap = modelMap - del.model
//    }
//  }


 def receiveRecover = {
   case x => "hej"
//    case x: String => ModelMakerAPI.readPF(x) {
//      case cm: ModelMakerAPI.CreateModel =>
//        reMod = reMod + (cm.model.get -> cm)
//      case dm: ModelMakerAPI.DeleteModel =>
//        reMod = reMod - dm.model
//    }
//    {PartialFunction.empty}
//    {x => println("Recover error in modelmaker. Got command: "+x)}
//
//    case RecoveryCompleted =>
//      reMod.values.foreach(addModel)
//      reMod = Map()
  }

}

object ModelMaker {
  def props(maker: ID => Props) = Props(classOf[ModelMaker], maker)
}