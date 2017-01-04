package sp.models

import akka.actor._

import scala.io.Source
import scala.util.{Failure, Success}




object Launch extends App {

  implicit val system = ActorSystem("SP")

  // Add root actors used in node here
  //system.actorOf(ModelMaker.props(ModelActor.props), "modelHandler")

  // Used only at one place in cluster for testing
  //system.actorOf(ClusterMonitor.props)

  // only for testing. Remove
    system.actorOf(Props(classOf[TestingWidget]), "testingWidget")
}

// testing actor
class TestingWidget extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator._
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("widgets", self)
  mediator ! Put(self)

  println("I'm running")

  def receive = {
    case x: String =>
      println("Testing widget got: "+x)
      scala.util.Try(new java.io.File(s"./gui/sp-example-widget/$x")) match {
        case Success(file) => sender() ! file
        case Failure(e) => sender() ! s"failed reading file: ${e.getMessage}"
      }
    case x => println("Testing widget got not as string: "+x)

  }

}



class ClusterMonitor extends Actor with ActorLogging {

  // special setup to monitor cluster
  import akka.cluster.Cluster
  import akka.cluster.ClusterEvent._
  import akka.cluster.pubsub._

  val cluster = Cluster(context.system)
  import DistributedPubSubMediator.{ Put, Subscribe, Publish }
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
