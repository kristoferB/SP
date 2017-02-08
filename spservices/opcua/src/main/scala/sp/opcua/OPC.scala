package sp.opcua

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


object OPC {
  def props(OpcUARuntime: ActorRef) = Props(classOf[OPC], OpcUARuntime)
}


// simple example opc ua client useage
class OPC(OpcUARuntime: ActorRef) extends Actor {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  val cp = "|var|CODESYS Control for Raspberry Pi SL.Application.Process_IOs."

  val nodes = List("feeder_exec", "newCylinder_var", "pnp1_mode", "pnp1from_var", "pnp1to_var", "p1_mode", "p1Transport_var", "p1Process_var", "convFree_var",
    "convMove_var", "convAtOut_var", "pnp2_mode", "pnp2to3_var", "pnp2to4_var", "p3_mode", "p3Process_var", "p4_mode", "p4Process_var")
  val nodeIDsToNode = nodes.map{n=>((cp+n)->n)}.toMap

  val ops = nodes.filter(_.contains("mode"))
  var resourceState = ops.map(_ -> false).toMap

  var connected = false
  val url = "opc.tcp://192.168.0.10:4840"

  mediator ! Subscribe("OPCState", self)

  self ! "connect"

  def receive = {
    case "connect" =>
      OpcUARuntime ! SPAttributes("cmd" -> "connect", "url" -> url)

    case attr: SPAttributes =>
      // check connection
      val connectionstatus = attr.getAs[Boolean]("connected").getOrElse(false)
      if(!connected && connectionstatus) {
        connected = true
        OpcUARuntime ! SPAttributes("cmd" -> "subscribe", "nodes" -> nodeIDsToNode.map(_._1))
      }
      if(!connected && !connectionstatus) {
        // try again in five seconds
        context.system.scheduler.scheduleOnce(Duration(5, TimeUnit.SECONDS), self, "connect")
      }

      val time = attr.getAs[String]("timeStamp").getOrElse("")
      val state = attr.getAs[Map[String, SPValue]]("state").getOrElse(Map())
      val shortMap = state.map(p=>nodeIDsToNode(p._1)->p._2).toMap
      if(time.nonEmpty && shortMap.nonEmpty) {
        println(shortMap)
        mediator ! Publish("raw", SPAttributes("state"->shortMap, "time" -> time).toJson)
      }
    case _ =>
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
