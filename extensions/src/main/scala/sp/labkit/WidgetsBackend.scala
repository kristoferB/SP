
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

object WidgetsBackend extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Labkit Widgets Backend"
    ))

  val transformTuple = ()
  val transformation = List()

  def props(eventHandler: ActorRef) = Props(classOf[WidgetsBackend], eventHandler)
}

case class OperationStarted(name: String, resource: String, product: String, operationType: String, time: String)
case class OperationFinished(name: String, resource: String, product: String, operationType: String, time: String)
case class ResourcePies(data: Map[String, Map[String, Int]])

class WidgetsBackend(eh: ActorRef) extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher

  val mediator = DistributedPubSub(system).mediator

  val serviceID = ID.newID
  val serviceName = "WidgetsBackend"

  val silent = SPAttributes("silent" -> true)

  mediator ! Subscribe("rawOperations", self)
  mediator ! Subscribe("summedOperations", self)
  mediator ! Subscribe("frontend", self)

  def receive = {

    case OperationStarted(name: String, resource: String, product: String, operationType: String, time: String) =>
      eh ! Response(List(), SPAttributes("operation"->name, "resource" -> resource, "type" -> operationType,
        "product" -> product, "executing" -> true, "startTime" -> time) merge silent, serviceName, serviceID)
    case OperationFinished(name: String, resource: String, product: String, operationType: String, time: String) =>
      eh ! Response(List(), SPAttributes("operation"->name, "resource" -> resource, "type" -> operationType,
        "product" -> product, "executing" -> false, "stopTime" -> time) merge silent, serviceName, serviceID)
    case ResourcePies(data) =>
      eh ! Response(List(), SPAttributes("pieData"->data) merge silent, serviceName, serviceID)
    case SummedOperations(state: Map[String,Int]) =>
      eh ! Response(List(), SPAttributes("summedOperations"->state) merge silent, serviceName, serviceID)

    case _ =>
      // sender ! SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
