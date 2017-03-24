package sp.service

import java.util.UUID

import akka.actor._
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._





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

  override def receive = {
    case x: String if sender() != self =>
      val message = SPMessage.fromJson(x)
      val header = for {m <- message; h <- m.getHeaderAs[SPHeader]} yield h
      val response = for {
        m <- message
        b <- m.getBodyAs[APISP.StatusResponse]
      } yield {
        val r = addResponse(b, sender())
      }






    case Tick =>
      val h = SPHeader("ServiceHandler")
      val b = APISP.StatusRequest(SPAttributes().addTimeStamp)
      val m = SPMessage.makeJson(h, b)
      mediator ! Publish("services", m)
  }



  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(5 seconds, 1 minute, self, Tick)

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
    val n = createName(resp)
    waitingResponse -= n
    services += n -> (resp, sender)
  }

  def deathWatch(actor: ActorRef) = {
    val re = services.filterNot(kv => kv._2._2 == actor).map(kv => kv._1 -> kv._2._1)
    services = services.filter(kv => re.contains(kv._1))
    waitingResponse = waitingResponse.filter(kv => re.contains(kv._1))
    re
  }


  def createName(x: APISP.StatusResponse ) = {
    val n = if (x.instanceName.isEmpty) x.instanceID.toString else x.instanceName
    x.service + "-" + n
  }


}
