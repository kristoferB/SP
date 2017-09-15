package sp.service

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator._
import sp.domain._

trait ServiceCommunicationSupport {
  var shComm: Option[ActorRef] = None
  def triggerServiceRequestComm(mediator: ActorRef, resp: APISP.StatusResponse, system: ActorSystem): Unit = {
    val x = system.actorOf(Props(classOf[ServiceHanderComm], mediator, resp))
    shComm = Some(x)
  }
  def updateServiceRequest(resp: APISP.StatusResponse): Unit = {
    shComm.foreach(_ ! resp)
  }

}

class ServiceHanderComm(mediator: ActorRef, resp: APISP.StatusResponse) extends Actor {
  var serviceResponse: APISP.StatusResponse = resp
  mediator ! Subscribe(APISP.serviceStatusRequest, self)
  sendEvent(SPHeader(from = serviceResponse.instanceName, to = APIServiceHandler.service))

  override def receive: Receive = {
    case x: APISP.StatusResponse if sender() != self => serviceResponse = x
    case x: String if sender() != self =>
      for {
        mess <- SPMessage.fromJson(x)
        h <- mess.getHeaderAs[SPHeader]
        b <- mess.getBodyAs[APISP] if b == APISP.StatusRequest
      } yield {
        sendEvent(h.copy(to = h.from, from = serviceResponse.instanceName))
      }
  }

  def sendEvent(h: SPHeader) =
    mediator ! Publish(APISP.serviceStatusResponse, SPMessage.makeJson(h, serviceResponse))
}
