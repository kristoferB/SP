package sp.service

import java.util.UUID

import akka.actor._
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._
import sp.service.APIServiceHandler.APIServiceHandler


package APIServiceHandler {
  sealed trait APIServiceHandler
  case class GetServices(names: List[String] = List(), ids: List[UUID] = List(), groups: List[String] = List()) extends APIServiceHandler

  case class Service(name: String,
                     id: Option[UUID] = None,
                     groups: List[String] = List(),
                     api: SPAttributes = SPAttributes(),
                     attributes: SPAttributes = SPAttributes())

  case class Services(xs: List[Service]) extends APIServiceHandler
  case class NewService(x: Service) extends APIServiceHandler
  case class RemovedService(x: Service) extends APIServiceHandler
}


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
    case x: String =>
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
      val m = SPMessage.makeJson(h, b).get  // should never fail since the macro should find problems during compile
      mediator ! Publish("services", m)
  }



  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(5 seconds, 1 minute, self, Tick)

}


trait ServiceHandlerLogic {
  var services: Map[String, (APIServiceHandler.Service, ActorRef)] = Map()
  var gotResponse: Map[String, APIServiceHandler.Service] = Map()
  var waitingResponse: Map[String, APIServiceHandler.Service] = Map()


  def aTick() = {

  }

  def addResponse(resp: APISP.StatusResponse, sender: ActorRef) = {
    val name = resp.attributes.getAs[String]("service").getOrElse("")
    val id = resp.attributes.getAs[UUID]("instanceID")
    val groups = resp.attributes.getAs[List[String]]("groups").getOrElse(List())
    val api = resp.attributes.getAs[SPAttributes]("api").getOrElse(SPAttributes())

    val ss = APIServiceHandler.Service(name, id, groups, api, resp.attributes)
    gotResponse += createName(name, id) -> ss
    services += createName(name, id) -> (ss, sender)
  }

  def deathWatch(actor: ActorRef) = {

  }


  def createName(name: String, id: Option[UUID]) = {
    if (name.isEmpty) "noName-" + id.getOrElse(UUID.randomUUID())
    else name + id.map(_.toString).getOrElse("")
  }


}
