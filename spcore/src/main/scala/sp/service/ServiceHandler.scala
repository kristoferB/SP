package sp.service

import java.util.UUID

import akka.actor._
import sp.domain._
import sp.domain.Logic._


import sp.service.APIServiceHandler._


/**
  * Created by kristofer on 2017-02-27.
  *
  * Monitors services and keeps track if they are removed or not behaving.
  *
  */
class ServiceHandler extends Actor with ServiceHandlerLogic {
  case object Tick

  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("services", self)

  override def receive = {
    case x: String if sender() != self =>
      val mess = SPMessage.fromJson(x)

      ServiceHandlerComm.extractRequest(mess).map{case (h, b) =>
        b match {
          case GetServices =>
            val res = services.map(_._2._1).toList
            val updH = h.copy(from = service, to = h.from)
            mediator ! Publish("answers", ServiceHandlerComm.makeMess(updH, Services(res)))
        }
      }

      ServiceHandlerComm.extractAPISP(mess).map{case (h, b) =>
        b match {
          case x: APISP.StatusResponse =>
            val res = addResponse(x, sender())
            context.watch(sender())
            if (res) {
              val h = SPHeader(from = service)
              mediator ! Publish("spevents", ServiceHandlerComm.makeMess(h, NewService(x)))
            }
          case doNothing => Unit
         }
      }

    case Terminated(ref) =>
      println("Removing service")
      val res = deathWatch(ref)
      val h = SPHeader(from = service)
      res.foreach{kv =>
        mediator ! Publish("spevents", ServiceHandlerComm.makeMess(h, RemovedService(kv._2)))
      }



    case Tick =>
      aTick()
      val h = SPHeader("ServiceHandler")
      val b = APISP.StatusRequest
      val m = SPMessage.makeJson(h, b)
      mediator ! Publish("services", m)
      //services.foreach(x => println(x._1))
  }



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


  def createName(x: APISP.StatusResponse ) = {
    val id = if (x.instanceID.isEmpty) "" else "-" +x.instanceID.get.toString
    val n = if (x.instanceName.isEmpty) id else "-" +x.instanceName
    x.service + n
  }


}
