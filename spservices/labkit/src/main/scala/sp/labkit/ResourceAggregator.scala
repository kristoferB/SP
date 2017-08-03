package sp.labkit

import akka.actor._
import sp.domain._
import sp.domain.Logic._

import scala.concurrent.Future
import akka.util._
import akka.pattern.ask

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put, Subscribe}
import java.util.concurrent.TimeUnit

import APIOPMaker._
import com.github.nscala_time.time.Imports.DateTime
import org.threeten.bp.ZonedDateTime

object ResourceAggregator {
  def props() = Props(classOf[ResourceAggregator])
}



class ResourceAggregator extends Actor {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  val processResources = List("p1","p3","p4")

  var processes: Map[String, Map[String, Int]] = processResources.map((_,Map():Map[String,Int])).toMap

  var baseTime: Int = -1

  mediator ! Subscribe("ops", self)

  def updateIdle = {
    val now = org.joda.time.DateTime.now.getMillis().intValue() - baseTime
    val toSend = processResources.map { resource =>
      val processTime = processes(resource).get("Process").getOrElse(0)
      val moveTime = Math.max(processes(resource).get("move").getOrElse(0) - processTime,0)
      val idleTime = Math.max(now - moveTime - processTime, 0)
      val m = Map("move" -> moveTime, "Process" -> processTime, "Idle" -> idleTime)
      (resource -> m)
    }
    mediator ! Publish("frontend", SPValue(APILabKitWidget_ResourcePies(toSend.toMap)).toJson)
  }

  def receive = {
    case "UpdateIdle" =>
      updateIdle
      context.system.scheduler.scheduleOnce(Duration(100, TimeUnit.MILLISECONDS), self, "UpdateIdle")

    case APIOPMaker_OP(start: APIOPMaker_OPEvent, end: Option[APIOPMaker_OPEvent], attributes: SPAttributes) =>
      val t = start.name
      val name = start.id
      val resource = start.resource
      val started = end.isEmpty
      val product = start.product.getOrElse("")

      // start tracking times on first event
      if(baseTime == -1) {
        baseTime = org.joda.time.DateTime.now.getMillis().intValue()
        self ! "UpdateIdle"
      }

      // TODO: SEND WITH HEADER IN THE FUTURE
      if(started) {
        // update gantt view
        mediator ! Publish("frontend", SPValue(APILabKitWidget_OperationStarted(name, resource, product, t, start.time.toString)).toJson)
      }
      else {
        // update gantt view
        mediator ! Publish("frontend", SPValue(APILabKitWidget_OperationFinished(name, resource, product, t, end.get.time.toString)).toJson)

        // update pie charts
        if(processResources.contains(resource)) {
          if(name.contains("Process")) {
            val duration = (end.get.time.getNano - start.time.getNano)/1000000
            val nt = processes(resource).get("Process").getOrElse(0) + duration
            val nm = processes(resource) + ("Process" -> nt)
            processes ++= Map(resource -> nm)
          }
          if(name.contains("move")) {
            val duration = (end.get.time.getNano - start.time.getNano)/1000000
            val nt = processes(resource).get("move").getOrElse(0) + duration
            val nm = processes(resource) + ("move" -> nt)
            processes ++= Map(resource -> nm)
          }
        }
      }

     case _ =>
   }

  def toDateTime(t: ZonedDateTime) = DateTime.parse(t.toString())

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
