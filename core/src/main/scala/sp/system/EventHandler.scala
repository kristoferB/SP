package sp.system

import akka.actor._
import akka.util.Timeout
import akka.event.Logging
import sp.system.messages._
import scala.concurrent.duration._
import sp.domain.Logic._

class EventHandler extends Actor {
  var sseChannels: List[ActorRef] = List()
  implicit val timeout = Timeout(5 seconds)
  import context.dispatcher
  val log = Logging(context.system, this)

  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Put(self)
  mediator ! Subscribe("eventHandler", self)

  def receive = {
    case e: SPEvent => sseChannels foreach( _ ! e )
    case SubscribeToSSE(ref) => {
      sseChannels = ref :: sseChannels
      println(s"sub to sse: $ref")
    }
    case UnSubscribeToSSE(ref) => {
      sseChannels = sseChannels.filterNot(_ == ref)
      println(s"UNsub to sse: $ref")

    }
    case other => println("not impl yet: "+ other)
  }
}

object EventHandler {
  def props = Props(classOf[EventHandler])
}
