package sp.areus

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import scala.annotation.tailrec
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import akka.camel._

import scala.util._

object MakeNewGanttTrajectory extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "areus",
      "description" -> "Tuning trajectories based on a Gantt"
    ),
    "rootHierarchy"-> KeyDefinition("ID", List(), None)

  )

  val transformTuple  = (
    TransformValue("rootHierarchy", _.getAs[ID]("rootHierarchy"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props = ServiceLauncher.props(Props(classOf[MakeNewGanttTrajectory]))
}

case class StartNEnd(start: Double, end: Double)
import sp.areus._
class MakeNewGanttTrajectory
  extends Actor 
  with ServiceSupport 
  with sp.areus.GanttTrajectoryLogic
  with sp.areus.TrajectoryLogic {

  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)
      //val amqConsumer = context.actorOf(ModalaAMQConsumer.props(replyTo,self, r, progress)) // start listening for replies

      progress ! SPAttributes("progress" -> "making a MODALA request")

      val trajectory = transform(MakeNewGanttTrajectory.transformTuple)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      val idMap = ids.map(x => x.id -> x).toMap
      val root = tryWithOption(idMap(trajectory).asInstanceOf[HierarchyRoot]).getOrElse(HierarchyRoot("empty"))
      val opsNChildren = getOperationsAndItsChildren(ids, root)
      val robotTrajectories = opsNChildren.map(_._2.filter(
        item => item.isInstanceOf[Operation] && item.attributes.getAs[List[Pose]]("poses").isDefined).
        map(_.asInstanceOf[Operation])).filter(_.nonEmpty).toList
      val makeSpan = getMakeSpan(robotTrajectories.flatten)
      val updateOps = matchGantt(robotTrajectories)

      val poses = updateOps.map{ops =>
        ops.flatMap(op => op.attributes.getAs[List[Pose]]("poses")).flatten.sortWith(_.time < _.time)
      }

      val newTraj = makeNewTrajectory(updateOps)
      replyTo ! Response(newTraj, SPAttributes("info"->"A trajectory from gantt"), service, reqID)

    }

    case _ => sender ! SPError("Ill formed request");
  }



}

trait GanttTrajectoryLogic {

  def getMakeSpan(xs: List[Operation]) = {
    val t = xs.map(x => x -> x.attributes.dig[Double]("gantt", "end").getOrElse(-1.0))
    t.maxBy(_._2)._2
  }

  def matchGantt(xs: List[List[Operation]]) = {
    xs.map{ops =>
      val init: (List[Operation], Double) = (List(), 0.0)
      val opNGantt = ops.foldLeft(init){(aggr, op) =>
        val prevOps = aggr._1
        val prevEnd = aggr._2
        val newPoses = for {
          gantt <- op.attributes.getAs[StartNEnd]("gantt")
          poses <- op.attributes.getAs[List[Pose]]("poses")
        } yield {
            val movedP = movePoses(poses, gantt.start)
            val extraP = extraPoses(movedP, prevEnd, gantt.start)
            (extraP ++ movedP, movedP.last.time)
          }
        val newThings = newPoses.getOrElse(List(), prevEnd)
        val updAttr = SPAttributes(op.attributes.obj.filter(_._1 != "poses")) + SPAttributes("poses"->newThings._1)
        (prevOps :+ op.copy(attributes = updAttr), newThings._2)
      }
      opNGantt._1
    }
  }

  def movePoses(poses: List[Pose], start: Double) = {
    if (poses.isEmpty || poses.head.time >= start) poses
    else {
      val diff = start - poses.head.time
      poses.map(p => p.copy(time = p.time + diff))
    }
  }

  def extraPoses(poses: List[Pose], start: Double, end: Double): List[Pose] = {
    if (poses.isEmpty || start >= end) List[Pose]()
    else {
      val diff = Math.abs((end - start) / 10)
      val joints = poses.head.joints
      var newPoses = List[Pose]()
      var time = start;
      while (time > end) {
        time += diff
        newPoses = newPoses :+ Pose(time, joints)
      }
      newPoses
    }
  }

  def makeNewTrajectory(xs: List[List[Operation]]): List[IDAble] = {
    val poses = xs.map{ops =>
      ops.flatMap(op => op.attributes.getAs[List[Pose]]("poses")).flatten.sortWith(_.time < _.time)
    }
    val markOps = xs.map{ops =>
      ops.flatMap(op => op.attributes.getAs[ZoneMark]("mark").map { x =>
        val poses = op.attributes.getAs[List[Pose]]("poses").get
        Thing(x.name, SPAttributes("mark" -> SPAttributes(
          "start"-> poses.head.time,
          "end" -> poses.last.time,
          "type" -> "zone"
        )))
      })
    }
    val newOps = poses.map(ps => Operation("newOp", List(), SPAttributes("poses"->ps)))
    val newThings = poses.map(ps => Thing("new"))
    val zip = (newOps zip markOps) zip newThings
    val chs = zip.map(z => HierarchyNode(z._2.id, List(HierarchyNode(z._1._1.id, z._1._2.map(t => HierarchyNode(t.id))))))
    List(HierarchyRoot("updated from Gantt", chs)) ++ newOps ++ newThings ++ markOps.flatten
  }
}

