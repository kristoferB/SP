package sp.models

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import akka.persistence._
import sp.domain._
import Logic._
import sp.service._



class ModelMaker(modelActorMaker: APIModelMaker.CreateModel => Props)
  extends PersistentActor with
    ActorLogging with
    ServiceCommunicationSupport with
    MessageBussSupport
{

  override def persistenceId = "modelMaker"
  import context.dispatcher

  private var modelMap: Map[ID, ActorRef] = Map()

  val instanceID = ID.newID
  val serviceInfo = ModelMakerInfo.attributes.copy(instanceID = Some(instanceID))

  subscribe(APIModelMaker.topicRequest)
  triggerServiceRequestComm(serviceInfo)

  // TODO: ModelMaker needs to check if there are already
  // running models and or modelmakers. Ask the serviceHandler
  // or ask on the modeltopic

  def receiveCommand = {
    case x: String if sender() != self =>
      for {
        m <- SPMessage.fromJson(x)
        h <- m.getHeaderAs[SPHeader] if h.to == instanceID.toString || h.to == APIModelMaker.service
        b <-   m.getBodyAs[APIModelMaker.Request]
      } yield {
        b match {
          case b: APIModelMaker.CreateModel =>
            val updH = h.copy(from = APIModelMaker.service, to = h.from)
            if (modelMap.contains(b.id)){
              sendAnswer(updH, APISP.SPError(s"Model ${b.id} already exist. Can not be created"))
            } else {
              sendAnswer(updH, APISP.SPACK())
              persist(x){ mess =>
                createModel(b)
                val res = APIModelMaker.ModelCreated(b.name, b.attributes, b.id)
                sendAnswer(updH, res)
                sendAnswer(updH, APISP.SPDone())
                //sendEvent(SPHeader(from = APIModelMaker.service), res)
              }

            }
          case b: APIModelMaker.DeleteModel =>
            val updH = h.copy(from = APIModelMaker.service, to = h.from)
            if (!modelMap.contains(b.id)){
              sendAnswer(updH, APISP.SPError(s"Model ${b.id} does not exists. Can not be deleted"))
            } else {
              sendAnswer(updH, APISP.SPACK())
              persist(x){mess =>
                deleteModel(b)
                val res = APIModelMaker.ModelDeleted(b.id)
                sendAnswer(updH, res)
                sendAnswer(updH, APISP.SPDone())
                //sendEvent(SPHeader(from = APIModelMaker.service), res)
              }

            }
          case APIModelMaker.GetModels =>
            val updH = h.copy(from = APIModelMaker.service, to = h.from)
            sendAnswer(updH, APIModelMaker.ModelList(modelMap.keys.toList))
            sendAnswer(updH, APISP.SPDone())
        }
      }
  }

  def createModel(model: APIModelMaker.CreateModel) = {
    val a = context.actorOf(modelActorMaker(model))
    modelMap += (model.id -> a)

  }

    def deleteModel(del: APIModelMaker.DeleteModel) = {
      if (modelMap.contains(del.id)){
        modelMap(del.id) ! PoisonPill
        modelMap = modelMap - del.id
      }
    }

 var models = Map[ID, APIModelMaker.CreateModel]()
 def receiveRecover = {
   case x: String =>
     val mess = SPMessage.fromJson(x)
     for {
       m <- mess
       b <-   m.getBodyAs[APIModelMaker.Request]
     } yield {
       b match {
         case b: APIModelMaker.CreateModel => models += (b.id -> b)
         case b: APIModelMaker.DeleteModel => models = models - b.id
         case _ => Unit
       }
     }

    case RecoveryCompleted =>
      models.values.foreach(createModel)
      models = Map()
  }


  def sendAnswer(h: SPHeader, b: APISP) = publish(APIModelMaker.topicResponse, SPMessage.makeJson(h, b))
  def sendAnswer(h: SPHeader, b: APIModelMaker.Response) = publish(APIModelMaker.topicResponse, SPMessage.makeJson(h, b))
  //def sendEvent(h: SPHeader, b: APIModelMaker.Response) = publish(APISP.spevents, SPMessage.makeJson(h, b))

}

object ModelMaker {
  def props(maker: APIModelMaker.CreateModel => Props) = Props(classOf[ModelMaker], maker)
}