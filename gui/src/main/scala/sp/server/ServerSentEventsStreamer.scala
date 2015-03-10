package sp.server

import scala.collection.mutable
import akka.actor._
import spray.can.Http
import spray.http.CacheDirectives.`no-cache`
import spray.http.HttpHeaders.`Cache-Control`
import spray.http._

object ServerSentEventsStreamer {
  def props(client: ActorRef, headers: List[HttpHeader]) = Props(classOf[ServerSentEventsStreamer], client, headers)
}

class ServerSentEventsStreamer(client: ActorRef, headers: List[HttpHeader]) extends Actor with ActorLogging {
  import ServerSentEventsStreamerProtocol._

  private val `text/event-stream` = MediaTypes.register(MediaType.custom("text/event-stream"))
  private val streamStart = " " * 2048
  private val responseStart = HttpResponse(
    entity = HttpEntity(`text/event-stream`, streamStart),
    headers = List(`Cache-Control`(`no-cache`)) ::: headers
  )

  private val messagesQueue = mutable.Queue[SSEMessage]()
  private var clientStatus: ClientStatus = ClientBusy

  client ! ChunkedResponseStart(responseStart).withAck(ClientReady)

  def receive = {
    case message: SSEMessage =>
      messagesQueue.enqueue(message)
      sendNextEventToClient()

    case ClientReady =>
      clientStatus = ClientReady
      sendNextEventToClient()

    case x: Http.ConnectionClosed =>
      clientStatus = ClientDead
  }

  private def sendNextEventToClient() = {
    clientStatus match {
      case ClientBusy => // do nothing
      case ClientReady =>
        if (messagesQueue.headOption.isDefined) {
          messagesQueue.dequeue() match {
            case e: Event =>
              client ! MessageChunk(stringifyEvent(e)).withAck(ClientReady)
            case EndOfEvents =>
              client ! ChunkedMessageEnd
              context.stop(self)
          }
          clientStatus = ClientBusy
        }
      case ClientDead =>
        if (messagesQueue.contains(EndOfEvents)) {
          context.stop(self)
        }
    }
  }

  private def stringifyEvent(event: Event) = Seq[String](
    event.id.map("id: " + _).getOrElse(""),
    event.event.map("event: " + _).getOrElse(""),
    event.data.split("\n").map("data: " + _).mkString("\n")
  ).filter(_.nonEmpty).mkString("\n") + "\n\n"

  sealed trait ClientStatus
  case object ClientReady extends ClientStatus
  case object ClientBusy extends ClientStatus
  case object ClientDead extends ClientStatus
}
