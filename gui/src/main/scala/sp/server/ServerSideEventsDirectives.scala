package sp.server

/**
 * Created by daniel on 2015-03-23.
 */

import akka.actor._
import sp.system.messages.{serverSentEndEvent, ServerSentDataEvent}
import spray.can.Http
import spray.http.HttpHeaders._
import spray.http.MediaTypes._
import spray.http._
import spray.routing.Directives._
import spray.routing._

// Enable scala features
import scala.language.{implicitConversions, postfixOps}
import scala.concurrent.duration._


trait ServerSideEventsDirectives {


  /*case class ServerSentDataEvent(data: String, event: Option[String], id: Option[String])
  object ServerSentEndEvent

  object ServerSentEvent {
    def apply(data: String) = ServerSentDataEvent(data, None, None)
    def apply(data: String, event: String) = ServerSentDataEvent(data, Some(event), None)
    def apply(data: String, event: String, id: String) = ServerSentDataEvent(data, Some(event), Some(id))
  }*/

  case class RegisterClosedHandler(handler: () => Unit)

  def sse(body: (ActorRef, Option[String]) => Unit)(implicit refFactory: ActorRefFactory): Route = {

    val responseStart = HttpResponse(
      headers = `Cache-Control`(CacheDirectives.`no-cache`) :: Nil,
      entity = ":" + (" " * 2049) + "\n" // 2k padding for IE using Yaffle
    )

    // TODO These headers should be standard headers
    val preflightHeaders = List(
      RawHeader("Access-Control-Allow-Methods", "GET"),
      RawHeader("Access-Control-Allow-Headers", "Last-Event-ID, Cache-Control"),
      RawHeader("Access-Control-Max-Age", "86400")
    )

    def lastEventId = optionalHeaderValueByName("Last-Event-ID") | parameter("lastEventId" ?)

    def sseRoute(lei: Option[String]) = (ctx: RequestContext) => {

      val connectionHandler = refFactory.actorOf(
        Props {
          new Actor {

            var closedHandlers: List[() => Unit] = Nil

            ctx.responder ! ChunkedResponseStart(responseStart)

            // Keep-Alive
            context.setReceiveTimeout(5 seconds)

            def receive = {
              case ServerSentDataEvent(data, event, id) =>
                val idString = id.map(id => s"id: $id\n").getOrElse("")
                val eventString = event.map(ev => s"event: $ev\n").getOrElse("")
                val dataString = data.split("\n").map(d => s"data: $d\n").mkString
                ctx.responder ! MessageChunk(s"${idString}${eventString}${dataString}\n")
              case `serverSentEndEvent` =>
                ctx.responder ! ChunkedMessageEnd
              case ReceiveTimeout =>
                ctx.responder ! MessageChunk("\n") // Comment to keep connection alive
              case RegisterClosedHandler(handler) => closedHandlers ::= handler
              case Http.Closed =>
                closedHandlers.foreach(_())
                context.stop(self)
            }
          }
        }
      )

      body(connectionHandler, lei)
    }

    val EventStreamType = register(
      MediaType.custom(
        mainType = "text",
        subType = "event-stream",
        compressible = true,
        binary = false
      )
    )

    get {
      respondWithMediaType(EventStreamType) {
        // TODO This should be a standard media type
        lastEventId { lei =>
          sseRoute(lei)
        }
      }
    } ~
    // Answer preflight requests. Needed for Yaffle
    method(HttpMethods.OPTIONS) {
      // TODO Change this with options, that it's included in Master
      respondWithHeaders(preflightHeaders: _*) {
        complete(StatusCodes.OK)
      }
    }
  }

}

object ServerSideEventsDirectives extends ServerSideEventsDirectives

