package sp.server

/**
 * Created by daniel on 2015-03-23.
 */

import akka.actor._
import akka.io.Tcp.PeerClosed
import sp.system.messages._
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

  case class RegisterClosedHandler(handler: () => Unit)

  def sse(channel: String, eventHandler: ActorSelection)(implicit refFactory: ActorRefFactory): Route = {
    val responseStart = HttpResponse(
      headers = `Cache-Control`(CacheDirectives.`no-cache`) :: Nil,
      entity = ":" + (" " * 2049) + "\n" // 2k padding for IE using Yaffle
    )

    def sseRoute(lei: String): Route = (ctx: RequestContext) => {
      refFactory.actorOf(
        Props {
          new Actor {
            ctx.responder ! ChunkedResponseStart(responseStart)
            context.setReceiveTimeout(5 seconds)
            eventHandler ! SubscribeToSSE(self)

            def endConnection = {
              ctx.responder ! ChunkedMessageEnd
              //ctx.complete("STOPPING SSE")
              eventHandler ! UnSubscribeToSSE(self)
              self ! PoisonPill
            }

            import sp.domain._
            def receive = {
              case e: SPEvent => {
                import sp.system.messages.JsonFormatsMessage._
                import org.json4s.native.JsonMethods._

                val data = org.json4s.native.JsonMethods.compact(render(SPValue(e)))
                val ev = e.getClass.getSimpleName
                ctx.responder ! MessageChunk(s"event: $ev\ndata:$data\n\n")
              }
              case "close" => {
                //ctx.responder ! ChunkedMessageEnd
                println("got sse close")
                endConnection
              }
              case ReceiveTimeout => {
                //ctx.responder ! ChunkedMessageEnd
                //println("got sse timeout")
                ctx.responder ! MessageChunk("\n")
              }
              case Http.Closed => {
                println("got HTTP closed")
                endConnection
              }

              case PeerClosed => {
                println("got Peer closed")
                endConnection
              }
              case x => {
                println("sse got "+x)
              }
            }
          }
        }
      )
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
        sseRoute(channel)
      }
    } //~
    // Answer preflight requests. Needed for Yaffle
//    method(HttpMethods.OPTIONS) {
//      val preflightHeaders = List(
//        RawHeader("Access-Control-Allow-Methods", "GET"),
//        RawHeader("Access-Control-Allow-Headers", "Last-Event-ID, Cache-Control"),
//        RawHeader("Access-Control-Max-Age", "86400")
//      )
//      respondWithHeaders(preflightHeaders: _*) {
//        complete(StatusCodes.OK)
//      }
//    }
  }
//  def lastEventId = optionalHeaderValueByName("Last-Event-ID") | parameter("lastEventId" ?)

}

object ServerSideEventsDirectives extends ServerSideEventsDirectives

