
package sp.labkit

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

  val serviceID = ID.newID
  val serviceName = "GanttBackend"
  var state: Map[String, String] = Map()

  val silent = SPAttributes("silent" -> true)

  eh ! SubscribeToSSE(self)

  var running = false

  Thread.sleep(5000)
  eh ! Response(List(), SPAttributes("resource"->"Resource 1", "executing" -> true) merge silent, serviceName, serviceID)
  Thread.sleep(5000)
  eh ! Response(List(), SPAttributes("resource"->"Resource 1", "executing" -> false, "stopTime" -> new DateTime()) merge silent, serviceName, serviceID)
  Thread.sleep(5000)

  def receive = {
    case Response(ids, attr, "OpcUARuntime", id) =>
      val state = attr.getAs[Map[String, SPValue]]("state").getOrElse(Map())
      // check state and send out response
      eh ! Response(List(), SPAttributes("resource"->true) merge silent, serviceName, serviceID)
    case _ =>
      // sender ! SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
