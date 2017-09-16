package sp.service

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator._
import sp.domain._


trait MessageBussSupport {
  val context: ActorContext
  val self: ActorRef
  private var bussCommActor: Option[ActorRef] = None

  def publish(topic: String, json: String): Unit = {
    init
    bussCommActor.foreach(_ ! PubSP(topic, json))
  }

  def subscribe(topic: String): Unit = {
    init
    bussCommActor.foreach(_ ! SubSP(topic))
  }

  def init = {
    if (bussCommActor.isEmpty) {
      // TODO: Add possible configuration here to use mediator, kafka or ROS etc.
      val x = context.actorOf(Props(classOf[MediatorMessageBus], self))
      bussCommActor = Some(x)
    }
  }
}

private case class SubSP(topic: String)
private case class PubSP(topic: String, json: String)

// Comm for the mediator
class MediatorMessageBus(sendTo: ActorRef) extends Actor {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator

  override def receive: Receive = {
    case SubSP(t) =>
      mediator ! Subscribe(t, self)
    case PubSP(t, json) =>
      mediator ! Publish(t, json)
    case x =>
      sendTo.forward(x)
  }
}
