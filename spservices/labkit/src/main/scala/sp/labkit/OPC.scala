package sp.labkit

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import org.json4s.JsonAST.{JValue,JBool,JInt,JString}
import org.json4s.DefaultFormats
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime
import sp.messages._
import scala.util.{Failure, Success, Try}

// to be able to use opcua runtime api
sealed trait API_OpcUARuntime
object API_OpcUARuntime {
  // requests
  case class Connect(url: String) extends API_OpcUARuntime
  case class Disconnect() extends API_OpcUARuntime
  case class GetNodes() extends API_OpcUARuntime
  case class Subscribe(nodeIDs: List[String]) extends API_OpcUARuntime
  case class Write(node: String, value: SPValue) extends API_OpcUARuntime

  // answers
  case class ConnectionStatus(connected: Boolean) extends API_OpcUARuntime
  case class AvailableNodes(nodes: Map[String, String]) extends API_OpcUARuntime
  case class StateUpdate(state: Map[String, SPValue], timeStamp: String) extends API_OpcUARuntime

  val service = "OpcUARuntime"
}

object OPC {
  def props = Props(classOf[OPC])
}


// simple example opc ua client useage
class OPC extends Actor {
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  val cp = "|var|CODESYS Control for Raspberry Pi SL.Application.Process_IOs."

  val nodes = List("feeder_exec", "newCylinder_var", "pnp1_mode", "pnp1from_var", "pnp1to_var", "p1_mode", "p1Transport_var", "p1Process_var", "convFree_var",
    "convMove_var", "convAtOut_var", "pnp2_mode", "pnp2to3_var", "pnp2to4_var", "p3_mode", "p3Process_var", "p4_mode", "p4Process_var")
  val nodeIDsToNode = nodes.map{n=>((cp+n)->n)}.toMap

  val ops = nodes.filter(_.contains("mode"))
  var resourceState = ops.map(_ -> false).toMap

  var connected = false
  val url = "opc.tcp://192.168.0.50:4840"

  mediator ! Subscribe("answers", self)
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("temp", self)


  self ! "connect"

  def receive = {
    case mess @ _ if {println(s"LABKIT MESSAGE: $mess from $sender"); false} => Unit

    case "connect" =>
      println("labkit: connecting to opc")
      val header = SPAttributes("to" -> API_OpcUARuntime.service, "replyID" -> ID.newID)
      val body = APIParser.writeJs(API_OpcUARuntime.Connect(url))
      val message = APIParser.write(SPMessage(header, body))
      mediator ! Publish("temp", message)
      mediator ! Publish("temp", "hej hopp")
      mediator ! Publish("services", "kalel stropp")



    case x: String =>
      // SPMessage uses the APIParser to parse the json string
      SPMessage.fromJson(x) match {
        case Success(mess) =>
          println(s"labkit: got ${mess.toString}")
          mediator ! Publish("services", "kalel stropp")
          getOPCUARuntimeMessage(mess).map{
            case API_OpcUARuntime.ConnectionStatus(connectionStatus) =>
              if(!connected && connectionStatus) {
                connected = true
                val header = SPAttributes("to" -> API_OpcUARuntime.service, "replyID" -> ID.newID)
                val body = APIParser.writeJs(API_OpcUARuntime.Subscribe(nodeIDsToNode.map(_._1).toList))
                val message = APIParser.write(SPMessage(header, body))
                mediator ! Publish("services", message)
              }
              if(!connectionStatus) {
                // try again in five seconds
                context.system.scheduler.scheduleOnce(Duration(5, TimeUnit.SECONDS), self, "connect")
              }
            case API_OpcUARuntime.StateUpdate(state: Map[String, SPValue], timeStamp: String) =>
              val shortMap = state.map(p=>nodeIDsToNode(p._1)->p._2).toMap
              mediator ! Publish("raw", SPAttributes("state"->shortMap, "time" -> timeStamp).toJson)
            case _ =>
          }
        case Failure(err) =>
      }
    case _ =>
  }

  def getMyMessage(spMess : SPMessage) = {
    val to = spMess.header.getAs[String]("to").getOrElse("") // extracts the header.to, if it is to me
    val body = Try{APIParser.readJs[API_OpcUARuntime](spMess.body)}
    if (body.isSuccess && to == API_OpcUARuntime.service)
      Some(body.get)
    else
      None
  }

  def getOPCUARuntimeMessage(spMess : SPMessage) = {
    val to = spMess.header.getAs[String]("from").getOrElse("") // answer from opcua runtime?
    val body = Try{APIParser.readJs[API_OpcUARuntime](spMess.body)}
    if (body.isSuccess && to == API_OpcUARuntime.service)
      Some(body.get)
    else
      None
  }


  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
