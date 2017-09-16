package sp.service

import akka.actor._
import sp.domain._


trait ServiceSupport extends ServiceCommunicationSupport with MessageBussSupport

trait ServiceCommunicationSupport {
  val context: ActorContext
  private var shComm: Option[ActorRef] = None
  def triggerServiceRequestComm(resp: APISP.StatusResponse): Unit = {
    if (shComm.isEmpty){
      val x = context.actorOf(Props(classOf[ServiceHandlerComm], resp))
      shComm = Some(x)
    }
  }
  def updateServiceRequest(resp: APISP.StatusResponse): Unit = {
    shComm.foreach(_ ! resp)
  }



}

class ServiceHandlerComm(resp: APISP.StatusResponse) extends Actor with MessageBussSupport {
  var serviceResponse: APISP.StatusResponse = resp
  subscribe(APISP.serviceStatusRequest)
  sendEvent(SPHeader(from = serviceResponse.instanceName, to = APIServiceHandler.service))

  override def receive: Receive = {
    case x: APISP.StatusResponse if sender() != self => serviceResponse = x
    case x: String if sender() != self =>
      for {
        mess <- SPMessage.fromJson(x)
        h <- mess.getHeaderAs[SPHeader]
        b <- mess.getBodyAs[APISP] if b == APISP.StatusRequest
      } yield {
        sendEvent(h.copy(to = h.from, from = serviceResponse.instanceID.toString))
      }
  }

  override def postStop() = {
    println("ServiceCommSupportActor closed for: " + serviceResponse.instanceID)
    publish(APISP.serviceStatusResponse,
      SPMessage.makeJson(
        SPHeader(to = APIServiceHandler.service, from = serviceResponse.instanceID.toString ),
        APIServiceHandler.RemoveService(serviceResponse)))
    super.postStop()
  }

  def sendEvent(h: SPHeader) =
    publish(APISP.serviceStatusResponse, SPMessage.makeJson(h, serviceResponse))
}
