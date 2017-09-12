package sp.models

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.concurrent.duration._
import akka.persistence._
import sp.domain._
import sp.domain.Logic._

import scala.util.{Failure, Success, Try}
import sp.models.{APIModelMaker => api}


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
  val serviceInfo = ModelMakerInfo.attributes.copy(instanceID = Some(instanceID))


  def receiveCommand = {
    case x: String if sender() != self =>
      val mess = SPMessage.fromJson(x)


      for {
        m <- mess
        h <- m.getHeaderAs[SPHeader] if h.to == instanceID.toString || h.to == api.service
        b <-   m.getBodyAs[api.Request]
      } yield {

        println("****")
        println("ModelMaker: " + mess)
        println("****")

        b match {
          case b: api.CreateModel =>
            val updH = h.copy(from = api.service, to = h.from)
            if (modelMap.contains(b.id)){
              sendAnswer(updH, APISP.SPError(s"Model ${b.id} already exist. Can not be created"))
            } else {
              sendAnswer(updH, APISP.SPACK())
              persist(x){mess =>
                createModel(b)
                sendAnswer(updH, api.ModelCreated(b.name, b.attributes, b.id))
                sendAnswer(updH, APISP.SPDone())
              }

            }
          case b: api.DeleteModel =>
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
          case api.GetModels =>
            val updH = h.copy(from = api.service, to = h.from)
            sendAnswer(updH, api.ModelList(modelMap.keys.toList))
            sendAnswer(updH, APISP.SPDone())
        }

      }


      ModelsComm.extractAPISP(mess).collect{
        case (h, APISP.StatusRequest) =>
          val updH = h.copy(from = api.service, to = h.from)
          val resp = serviceInfo

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
     for {
       m <- mess
       b <-   m.getBodyAs[api.Request]
     } yield {
       b match {
         case b: api.CreateModel => models += (b.id -> b)
         case b: api.DeleteModel => models = models - b.id
         case _ => Unit
       }
     }

    case RecoveryCompleted =>
      models.values.foreach(createModel)
      models = Map()
  }


  def sendAnswer(h: SPHeader, b: APISP) = mediator ! Publish(APISP.answers, SPMessage.makeJson(h, b))
  def sendAnswer(h: SPHeader, b: api.Response) = mediator ! Publish(APISP.answers, SPMessage.makeJson(h, b))
  def sendEvent(h: SPHeader, b: APISP) = mediator ! Publish(APISP.spevents, SPMessage.makeJson(h.copy(to = ""), b))
  def sendEvent(h: SPHeader, b: api.Response) = mediator ! Publish(APISP.spevents, SPMessage.makeJson(h.copy(to = ""), b))

}

object ModelMaker {
  def props(maker: api.CreateModel => Props) = Props(classOf[ModelMaker], maker)
}