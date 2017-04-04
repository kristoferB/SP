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
  mediator ! Subscribe(APISP.services, self)
  mediator ! Put(self)

  private var modelMap: Map[ID, ActorRef] = Map()

  val instanceID = ID.newID.toString


  def receiveCommand = {
    case x: String if sender() != self =>
      val mess = SPMessage.fromJson(x)

      ModelsComm.extractRequest(mess, instanceID).toOption.collect{
        case (h, b: api.CreateModel) =>
          val updH = h.copy(from = api.attributes.service, to = h.from)
          if (modelMap.contains(b.id)){
            mediator ! Publish(APISP.answers, ModelsComm.makeMess(updH, APISP.SPError(s"Model ${b.id} already exist. Can not be created")))
          } else {
            mediator ! Publish(APISP.answers, ModelsComm.makeMess(updH, APISP.SPACK()))
            persist(x){mess =>
              createModel(b)
              mediator ! Publish(APISP.spevents, ModelsComm.makeMess(updH, api.ModelDeleted(b.id)))
              mediator ! Publish(APISP.answers, ModelsComm.makeMess(updH, APISP.SPDone()))
            }

          }
        case (h, b: api.DeleteModel) =>
          val updH = h.copy(from = api.attributes.service, to = h.from)
          if (!modelMap.contains(b.id)){
            mediator ! Publish(APISP.answers, ModelsComm.makeMess(updH, APISP.SPError(s"Model ${b.id} does not exists. Can not be deleted")))
          } else {
            mediator ! Publish(APISP.answers, ModelsComm.makeMess(updH, APISP.SPACK()))
            persist(x){mess =>
              deleteModel(b)
              mediator ! Publish(APISP.answers, ModelsComm.makeMess(updH, APISP.SPDone()))
            }

          }
        case (h, b: api.GetModels) =>
          val mess = ModelsComm.makeMess(h.copy(from = api.attributes.service, to = h.from), api.ModelList(modelMap.keys.toList))
          mediator ! Publish(APISP.answers, mess)
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
  def props(maker: api.CreateModel => Props) = Props(classOf[ModelMaker], maker)
}