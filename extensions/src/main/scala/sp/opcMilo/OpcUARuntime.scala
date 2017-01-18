package sp.opcMilo

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties

object OpcUARuntime extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "OPCUA runtime"
    ))

  val transformTuple = ()
  val transformation = List()

  def props(eventHandler: ActorRef) = Props(classOf[OpcUARuntime], eventHandler)
}

// simple example opc ua client useage
class OpcUARuntime(eh: ActorRef) extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher

  val serviceID = ID.newID
  val serviceName = "OpcUARuntime"
  var client = new MiloOPCUAClient()
  var state = State(Map())
  var idToIdentifier: Map[ID, String] = Map()

  val silent = SPAttributes("silent" -> true)
  def connectionAttr = SPAttributes("connected" -> client.isConnected)

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)

      progress ! SPAttributes("progress" -> "starting opcrunner")

      val cmd = attr.getAs[String]("cmd").getOrElse("")

      cmd match {
        case "connect" =>
          if(client.isConnected)
            replyTo ! Response(List(), connectionAttr, rnr.req.service, rnr.req.reqID)
          else {
            val address = attr.getAs[String]("url").getOrElse("opc.tcp://localhost:12686")
            if(!client.connect(address)) {
              replyTo ! SPError("Could not connect to server")
            } else {
              replyTo ! Response(List(),connectionAttr, rnr.req.service, rnr.req.reqID)
            }
          }
        case "disconnect" =>
          if(client.isConnected) client.disconnect()
          replyTo ! Response(List(),connectionAttr merge silent, rnr.req.service, rnr.req.reqID)
        case "getNodes" =>
          val nodes = client.getAvailableNodes.map { case (i,dt) => (i,dt.toString) }.toMap
          replyTo ! Response(List(), SPAttributes("nodes"->nodes) merge silent, rnr.req.service, rnr.req.reqID)
        case "subscribe" =>
          val nodes = attr.getAs[List[String]]("nodes").getOrElse(List())
          client.subscribeToNodes(nodes, self)
        case "write" =>
          val node = attr.getAs[String]("node").getOrElse("")
          val value = attr.getAs[SPValue]("value").getOrElse(SPValue(false))
          client.write(node, value)
      }
    }
    case StateUpdate(activeState) =>
      eh ! Response(List(), SPAttributes("state"->activeState) merge silent, serviceName, serviceID)
    case _ => sender ! SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
