
package sp.labkit

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import org.json4s.JsonAST.{JValue,JBool,JInt,JString}
import org.json4s.DefaultFormats
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
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }

import org.joda.time.DateTime

object OPC extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Gantt Backend"
    ))

  val transformTuple = ()
  val transformation = List()

  def props(opc: ActorRef) = Props(classOf[OPC], opc)
}

case class OperationStarted(name: String, resource: String, product: String, operationType: String, time: String)
case class OperationFinished(name: String, resource: String, product: String, operationType: String, time: String)

// simple example opc ua client useage
class OPC(opc: ActorRef) extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  val serviceID = ID.newID
  val serviceName = "OPC"

  val cp = "|var|CODESYS Control for Raspberry Pi SL.Application.Process_IOs."

  val nodes = List("feeder_exec", "newCylinder_var", "pnp1_mode", "pnp1from_var", "pnp1to_var", "p1_mode", "p1Transport_var", "p1Process_var", "convFree_var",
    "convMove_var", "convAtOut_var", "pnp2_mode", "pnp2to3_var", "pnp2to4_var", "p3_mode", "p3Process_var", "p4_mode", "p4Process_var")
  val nodeIDsToNode = nodes.map{n=>((cp+n)->n)}.toMap

  val ops = nodes.filter(_.contains("mode"))
  var resourceState = ops.map(_ -> false).toMap

  // TODO: check reply that we actually connect...
  opc ! SPAttributes("cmd" -> "connect", "url" -> "opc.tcp://192.168.0.50:4840")

  Thread.sleep(5000)

  opc ! SPAttributes("cmd" -> "subscribe", "nodes" -> nodeIDsToNode.map(_._1))

  mediator ! Subscribe("OPCState", self)

  def receive = {
    case attr: SPAttributes =>
      val time = attr.getAs[String]("timeStamp").getOrElse("")
      val state = attr.getAs[Map[String, SPValue]]("state").getOrElse(Map())
      val shortMap = state.map(p=>nodeIDsToNode(p._1)->p._2).toMap
      println(shortMap)
      mediator ! Publish("raw", SPAttributes("state"->shortMap, "time" -> time).toJson)
      // shortMap.filter{case (k,v) => ops.contains(k)}. map { case (name,v) =>
      //   val bool = v == JInt(2)
      //   if(bool && !resourceState(name)) {
      //     // op started
      //     resourceState = resourceState + (name -> true)
      //     mediator ! Publish("rawOperations", OperationStarted(name, time))
      //   }
      //   if(!bool && resourceState(name)) {
      //     // op started
      //     resourceState = resourceState + (name -> false)
      //     mediator ! Publish("rawOperations", OperationFinished(name, time))
      //   }
      // }
    case _ =>
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
