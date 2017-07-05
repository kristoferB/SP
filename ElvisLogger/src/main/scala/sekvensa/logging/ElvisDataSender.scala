package sekvensa.logging

import akka.actor._

class ElvisDataSender extends Actor {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{Publish}
  val mediator = DistributedPubSub(context.system).mediator

  override def receive = {
    case playBack: List[String] => {
      println("******** START ************")
      println("[")
      playBack.foreach { x =>
        mediator ! Publish("historical-elvis-data", x)
        Thread.sleep(1)
      }
      println("]")
      println("******** STOP ************")
    }
  }
}
