
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
import sp.system.SPActorSystem.system

import org.joda.time.DateTime

object OPC extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Gantt Backend"
    ))

  val transformTuple = ()
  val transformation = List()

  def props(sh: ActorRef) = Props(classOf[OPC], sh)
}

case class OperationStarted(name: String, time: String)
case class OperationFinished(name: String, time: String)

// simple example opc ua client useage
class OPC(sh: ActorRef) extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher
  val mediator = DistributedPubSub(system).mediator

  val serviceID = ID.newID
  val serviceName = "OPC"

  val ops = Map("Resource 1" -> "126,=V1AR171+UC2171-XP3:A8",
    "Resource 2" -> "126,=V1AR171+UC2171-XP3:B8",
    "Resource 3" -> "126,=V1AR31+UC231-XP3:A11",
    "Resource 4" -> "126,=V1AR31+UC231-XP3:A12")

  val nodeToResource = ops.map{ case (r,n) => (n -> r)}.toMap
  var resourceState = ops.map(_._1 -> false).toMap

  // check reply that we actually connect...
  sh ! Request("OpcUARuntime", SPAttributes("cmd" -> "connect", "url" -> "opc.tcp://localhost:12686"), List(), ID.newID)

  Thread.sleep(5000)

  sh ! Request("OpcUARuntime", SPAttributes("cmd" -> "subscribe", "nodes" -> ops.map(_._2)), List(), ID.newID)

  mediator ! Subscribe("OPCState", self)

  def receive = {
    case attr: SPAttributes =>
      val time = attr.getAs[String]("timeStamp").getOrElse("")
      val state = attr.getAs[Map[String, SPValue]]("state").getOrElse(Map())
      println("got state: " + state + " at time " + time)
      state.map{ case (s,v) =>
        val resource = nodeToResource(s)
        val bool = v == JBool(true)
        println("res: " + resource + " bool: " + bool)
        if(bool && !resourceState(resource)) {
          // op started
          resourceState = resourceState + (resource -> true)
          mediator ! Publish("rawOperations", OperationStarted(resource, time))
          println("published executing state")
        }
        if(!bool && resourceState(resource)) {
          // op started
          resourceState = resourceState + (resource -> false)
          mediator ! Publish("rawOperations", OperationFinished(resource, time))
          println("published ended state")
        }
      }
    case _ =>
      // sender ! SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
