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
import org.joda.time.DateTime

import APIOPMaker._

object ResourceAggregator {
  def props() = Props(classOf[ResourceAggregator])
}

class ResourceAggregator extends Actor {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  mediator ! Subscribe("ops", self)

  def receive = {
    case OP(start: OPEvent, end: Option[OPEvent], attributes: SPAttributes) =>
      val t = start.name
      val name = start.id
      val resource = start.resource
      val started = end.isEmpty

      if(started)
        mediator ! Publish("frontend", OperationStarted(name, resource, "", t, start.time.toString))
      else
        mediator ! Publish("frontend", OperationFinished(name, resource, "", t, end.get.time.toString))



     case _ =>
   }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
