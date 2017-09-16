package sp.service

import akka.actor._
import sp.domain._
import sp.domain.Logic._


/**
  * Created by kristofer on 2017-02-27.
  *
  * Monitors services and keeps track if they are removed or not behaving.
  *
  */
class ServiceHandler extends Actor with ServiceHandlerLogic with MessageBussSupport {
  case object Tick

  subscribe(APIServiceHandler.topicRequest)
  subscribe(APISP.serviceStatusResponse)

  override def receive = {
    case x: String if sender() != self =>
      val mess = SPMessage.fromJson(x)

      for {
        m <- mess
        h <- m.getHeaderAs[SPHeader] if h.to == APIServiceHandler.service
        b <- m.getBodyAs[APIServiceHandler.Request]
      } yield { b match {
          case APIServiceHandler.GetServices =>
            val res = services.map(_._2._1).toList
            val updH = h.copy(from = APIServiceHandler.service, to = h.from)
            sendAnswer(updH, APIServiceHandler.Services(res))
          case APIServiceHandler.RemoveService(sR) =>
            removeService(sR)
            sendAnswer(SPHeader(from = APIServiceHandler.service), APIServiceHandler.ServiceRemoved(sR))
        }
      }

      for {
        m <- mess
        h <- m.getHeaderAs[SPHeader]
        b <- m.getBodyAs[APISP] if b.isInstanceOf[APISP.StatusResponse]
      } yield {
        val sR = b.asInstanceOf[APISP.StatusResponse]
        val res = addResponse(sR, sender())
        context.watch(sender())
        if (res) {
          val h = SPHeader(from = APIServiceHandler.service)
          sendAnswer(h, APIServiceHandler.ServiceAdded(sR))
        }
      }

    // Watching all services that are actors. Other services should send a
    // APIServiceHandler.RemoveService
    case Terminated(ref) =>
      println("Removing service")
      val res = deathWatch(ref)
      res.foreach{kv =>
        sendAnswer(SPHeader(from = APIServiceHandler.service), APIServiceHandler.ServiceRemoved(kv._2))
      }

    case Tick =>
      aTick()
      val h = SPHeader("ServiceHandler")
      val b = APISP.StatusRequest
      sendReq(h, b)
  }

  def sendReq(h: SPHeader, b: APISP) = publish(APISP.serviceStatusRequest, SPMessage.makeJson(h, b))
  def sendAnswer(h: SPHeader, b: APIServiceHandler.Response) = publish(APIServiceHandler.topicResponse, SPMessage.makeJson(h, b))


  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(5 seconds, 5 seconds, self, Tick)

}

object ServiceHandler {
  def props = Props(classOf[ServiceHandler])
}


trait ServiceHandlerLogic {
  var services: Map[String, (APISP.StatusResponse, ActorRef)] = Map()
  var waitingResponse: Map[String, APISP.StatusResponse] = Map()


  def aTick() = {
    val noAnswer = waitingResponse.map{kv =>
      services -= kv._1
      kv
    }
    waitingResponse = services.map(kv => kv._1 -> kv._2._1)
    noAnswer
  }

  def addResponse(resp: APISP.StatusResponse, sender: ActorRef) = {
    val re = services.filter(kv => kv._2._2 == sender).map(kv => kv._1 -> kv._2._1)
    val n = if (re.isEmpty) createName(resp) else re.head._1

    val res = !services.contains(n)
    waitingResponse -= n
    services += n -> (resp, sender)
    res
  }

  def deathWatch(actor: ActorRef) = {
    val re = services.filter(kv => kv._2._2 == actor).map(kv => kv._1 -> kv._2._1)
    services = services.filterNot(kv => re.contains(kv._1))
    waitingResponse = waitingResponse.filterNot(kv => re.contains(kv._1))
    re
  }

  def removeService(sR: APISP.StatusResponse): Unit = {
    val n = createName(sR)
    services = services - n
    waitingResponse = waitingResponse - n
  }


  def createName(x: APISP.StatusResponse ) = {
    val n = if (x.instanceName.isEmpty) "" else "-" +x.instanceName
    val id = if (x.instanceID.isEmpty) n else "-" +x.instanceID.get.toString
    x.service + id
  }


}
