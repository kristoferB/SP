package sp.system

import akka.actor._
import akka.util.Timeout
import akka.event.Logging
import sp.system.messages._
import scala.concurrent.duration._
import sp.domain.Logic._

class EventHandler extends Actor {
  private var sseChannels: List[ActorRef] = List.empty
  var lastEventID: Int = 0
  implicit val timeout = Timeout(5 seconds)
  import context.dispatcher
  val log = Logging(context.system, this)

  def receive = {
    case e: EventMessage => sendEvent(e, e.target)
    case ss: SubscribeToSSE => sseChannels = ss.channel :: sseChannels
    case other @ _ => println("not impl yet: ", other)
  }

  def sendEvent(data: EventMessage, event: String): Unit = {
    val sse = ServerSentEvent(org.json4s.native.Serialization.write(data).toString, event, lastEventID.toString)
    for {
      channel <- sseChannels
    } yield {
      channel ! sse
    }
    lastEventID += 1
  }
  
}

object EventHandler {
  def props = Props(classOf[EventHandler])
}
