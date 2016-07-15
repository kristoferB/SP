package sp.models

import akka.actor._



object Launch extends App {

  implicit val system = ActorSystem("SP")

  // Add root actors used in node here
  system.actorOf(ModelMaker.props(ModelActor.props), "modelHandler")

  // Used only at one place in cluster for testing
  system.actorOf(ClusterMonitor.props)


}


class ClusterMonitor extends Actor with ActorLogging {

  // special setup to monitor cluster
  import akka.cluster.Cluster
  import akka.cluster.ClusterEvent._
  import akka.cluster.pubsub._

  val cluster = Cluster(context.system)
  import DistributedPubSubMediator.{ Put, Subscribe }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("modelMessages", self)
  mediator ! Subscribe("modelEvents", self)

  def receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
    case _: MemberEvent => // ignore
    case mess =>
      println(s"Message was sent: $mess")
  }

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)
}

object ClusterMonitor {
  def props = Props(classOf[ModelMaker])
}
