package sp.models

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.concurrent.duration._
import akka.persistence._
import sp.domain._
import sp.domain.Logic._

import scala.util.{Failure, Success, Try}
import sp.models.{APIModel => api}


class ModelMaker(modelActorMaker: api.CreateModel => Props) extends PersistentActor with ActorLogging  {
  override def persistenceId = "modelMaker"
  import context.dispatcher
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Subscribe }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(APISP.services, self)
  mediator ! Put(self)

  private var modelMap: Map[ID, ActorRef] = Map()

  val instanceID = ID.newID


  def receiveCommand = {
    case x: String if sender() != self =>
      val mess = SPMessage.fromJson(x)

      ModelsComm.extractRequest(mess, api.service, instanceID).collect{
        case (h, b: api.CreateModel) =>
          val updH = h.copy(from = api.service, to = h.from)
          if (modelMap.contains(b.id)){
            sendAnswer(updH, APISP.SPError(s"Model ${b.id} already exist. Can not be created"))
          } else {
            sendAnswer(updH, APISP.SPACK())
            persist(x){mess =>
              createModel(b)
              sendAnswer(updH, APISP.SPDone())
            }

          }
        case (h, b: api.DeleteModel) =>
          val updH = h.copy(from = api.service, to = h.from)
          if (!modelMap.contains(b.id)){
            sendAnswer(updH, APISP.SPError(s"Model ${b.id} does not exists. Can not be deleted"))
          } else {
            sendAnswer(updH, APISP.SPACK())
            persist(x){mess =>
              deleteModel(b)
              sendEvent(updH, api.ModelDeleted(b.id))
              sendAnswer(updH, APISP.SPDone())
            }

          }
        case (h, b: api.GetModels) =>
          val updH = h.copy(from = api.service, to = h.from)
          sendAnswer(updH, api.ModelList(modelMap.keys.toList))
          sendAnswer(updH, APISP.SPDone())
      }


      ModelsComm.extractAPISP(mess).collect{
        case (h, APISP.StatusRequest) =>
          val updH = h.copy(from = api.service, to = h.from)
          val resp = APISP.StatusResponse(
            service = api.service,
            instanceID = Some(instanceID),
            tags = List("models", "modelhandler"),
            attributes = SPAttributes("models" -> modelMap.keys.toList)
          )

          sendEvent(updH, resp)

      }

    case x: api.DeleteModel =>


  }

  def createModel(model: api.CreateModel) = {
    val a = context.actorOf(modelActorMaker(model))
    modelMap += (model.id -> a)
  }

    def deleteModel(del: api.DeleteModel) = {
      if (modelMap.contains(del.id)){
        modelMap(del.id) ! PoisonPill
        modelMap = modelMap - del.id
      }
    }



  var models = Map[ID, api.CreateModel]()
 def receiveRecover = {
   case x: String =>

     val mess = SPMessage.fromJson(x)

     ModelsComm.extractRequest(mess, api.service, instanceID).map{
      case (h, b: api.CreateModel) => models += (b.id -> b)
      case (h, b: api.DeleteModel) => models = models - b.id
      case _ => Unit
     }


    case RecoveryCompleted =>
      models.values.foreach(createModel)
      models = Map()
  }


  def sendAnswer(h: SPHeader, b: APISP) = mediator ! Publish(APISP.answers, ModelsComm.makeMess(h, b))
  def sendAnswer(h: SPHeader, b: api.Response) = mediator ! Publish(APISP.answers, ModelsComm.makeMess(h, b))
  def sendEvent(h: SPHeader, b: APISP) = mediator ! Publish(APISP.spevents, ModelsComm.makeMess(h.copy(to = ""), b))
  def sendEvent(h: SPHeader, b: api.Response) = mediator ! Publish(APISP.spevents, ModelsComm.makeMess(h.copy(to = ""), b))

}

object ModelMaker {
  def props(maker: api.CreateModel => Props) = Props(classOf[ModelMaker], maker)
}