
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

object GanttBackend extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Gantt Backend"
    ))

  val transformTuple = ()
  val transformation = List()

  def props(eventHandler: ActorRef) = Props(classOf[GanttBackend], eventHandler)
}

// simple example opc ua client useage
class GanttBackend(eh: ActorRef) extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher

  val mediator = DistributedPubSub(system).mediator

  val serviceID = ID.newID
  val serviceName = "GanttBackend"

  val silent = SPAttributes("silent" -> true)

  mediator ! Subscribe("rawOperations", self)

  def receive = {

    case OperationStarted(name: String, time: String) =>
      eh ! Response(List(), SPAttributes("resource"->name, "executing" -> true) merge silent, serviceName, serviceID)

    case OperationFinished(name: String, time: String) =>
      eh ! Response(List(), SPAttributes("resource"->name, "executing" -> false, "stopTime" -> time) merge silent, serviceName, serviceID)

    case _ =>
      // sender ! SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
