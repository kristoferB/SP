package sp.opcMilo

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties
import org.joda.time.DateTime
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
import sp.messages._

object OpcUARuntime {
  def props = Props(classOf[OpcUARuntime])
}

// simple example opc ua client useage
class OpcUARuntime extends Actor {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator
  val topic = "OPCState"

  var client = new MiloOPCUAClient()
  var state = State(Map())
  var idToIdentifier: Map[ID, String] = Map()

  def connectionAttr = SPAttributes("connected" -> client.isConnected)

  def receive = {
    case attr: SPAttributes => {
      val replyTo = sender()
      val cmd = attr.getAs[String]("cmd").getOrElse("")
      cmd match {
        case "connect" =>
          if(client.isConnected)
            replyTo ! connectionAttr
          else {
            val address = attr.getAs[String]("url").getOrElse("opc.tcp://localhost:12686")
            if(!client.connect(address)) {
              replyTo ! SPAttributes("error"->"Could not connect to server")
            } else {
              replyTo ! connectionAttr
            }
          }
        case "disconnect" if client.isConnected =>
          client.disconnect()
          replyTo ! connectionAttr
        case "getNodes" if client.isConnected =>
          val nodes = client.getAvailableNodes.map { case (i,dt) => (i,dt.toString) }.toMap
          replyTo ! SPAttributes("nodes"->nodes)
        case "subscribe" if client.isConnected =>
          val nodes = attr.getAs[List[String]]("nodes").getOrElse(List())
          client.subscribeToNodes(nodes, self)
        case "write" if client.isConnected =>
          val node = attr.getAs[String]("node").getOrElse("")
          val value = attr.getAs[SPValue]("value").getOrElse(SPValue(false))
          client.write(node, value)
      }
    }
    case StateUpdate(activeState) =>
      mediator ! Publish(topic, SPAttributes("state"->activeState, "timeStamp" -> client.getCurrentTime.toString))
    case _ => sender ! APISP.SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
