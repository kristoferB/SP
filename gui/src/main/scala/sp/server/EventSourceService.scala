package sp.server

import akka.actor.{ ActorRef, ActorRefFactory }
import akka.pattern.after
import spray.http.HttpHeader
import spray.routing.{ HttpService, RequestContext }
import ServerSentEventsStreamerProtocol._

trait EventSourceService extends HttpService {

  val eventSourceRoute = path("eventsource") {
    get { implicit ctx =>
      val stream = serverSentEventsStreamer()
      stream ! Event(Some(1), Some("stateChange"), "R2 at Table")
      stream ! Event(Some(2), Some("stateChange"), "R2 moving")
      stream ! EndOfEvents

    }
  }

  def serverSentEventsStreamer(headers: List[HttpHeader] = List())(implicit ctx: RequestContext, actorRefFactory: ActorRefFactory): ActorRef = {
    actorRefFactory.actorOf(ServerSentEventsStreamer.props(ctx.responder, headers))
  }
}