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

object OperationSummer extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Gantt Backend"
    ))

  val transformTuple = ()
  val transformation = List()

  def props = Props(classOf[OperationSummer])
}

case class SummedOperations(state: Map[String,Int])

// simple example opc ua client useage
class OperationSummer extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher
  val mediator = DistributedPubSub(system).mediator

  val serviceID = ID.newID
  val serviceName = "OperationSummer"

  var state: Map[String,Int] = Map()

  mediator ! Subscribe("rawOperations", self)

  def receive = {
    // case OperationFinished(name: String, time: String) =>
    //   state = state + (name -> (state.get(name).getOrElse(0) + 1))
    //   mediator ! Publish("summedOperations", SummedOperations(state))
     case _ =>
   }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
