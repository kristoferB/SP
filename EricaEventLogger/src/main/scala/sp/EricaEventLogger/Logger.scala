package sp.EricaEventLogger

import akka.actor._
import akka.persistence._

object Logger {
  def props = Props(classOf[Logger])
}


class Logger extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("historical-elvis-data", self)

  override def receive = {
    case x => println("received something: " + x)
  }

}
