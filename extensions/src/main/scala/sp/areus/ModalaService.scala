package sp.areus.modalaService

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

case class ModalaSetup(inputdata: String, rootHierarchy: ID)

object ModalaService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "DALAHÄSTAR"
    ),
    "setup" -> SPAttributes(
      "inputdata" -> KeyDefinition("String", List(), Some("")),
      "rootHierarchy"-> KeyDefinition("ID", List(), None)
    )
  )

  val transformTuple  = (
    TransformValue("setup", _.getAs[ModalaSetup]("setup"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(amqProducer: ActorRef) = ServiceLauncher.props(Props(classOf[ModalaService], amqProducer))
}

// forward requests to Modala via active mq
class ModalaAMQProducer extends Actor with Producer {
  def endpointUri = "activemq:MODALA.QUERIES"
  override def oneway: Boolean = true
}

// listen to responses from modala via active mq
class ModalaAMQConsumer(caller : ActorRef, service : ActorRef, req : Request, progress : ActorRef) extends Actor with Consumer {
  implicit val timeout = Timeout(100 seconds)
  def endpointUri = "activemq:MODALA.RESPONSES"

  def receive = {
    case msg: CamelMessage => {
      println(s"JAA: $msg")
      msg.body match {
        case "progress" => {
          // relay progress messages
          progress ! SPAttributes("progress" -> "Modala has some new info...")
        }
        case _ => {
          // final result, relay back to main service...
          service ! DalaResponse(msg.body.toString, caller, req, progress)
        }
      }
    }
  }  
}

object ModalaAMQProducer {
  def props = Props(classOf[ModalaAMQProducer])
}

object ModalaAMQConsumer {
  def props(caller : ActorRef, service: ActorRef, req : Request, progress : ActorRef) = Props(classOf[ModalaAMQConsumer],caller,service,req, progress)
}

case class DalaResponse(message : String, caller : ActorRef, req : Request, progress : ActorRef)
import sp.areus._
class ModalaService(amqProducer: ActorRef) extends Actor with ServiceSupport with sp.areus.TrajectoryLogic {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)
      val amqConsumer = context.actorOf(ModalaAMQConsumer.props(replyTo,self, r, progress)) // start listening for replies

      progress ! SPAttributes("progress" -> "making a MODALA request")

      val setup = transform(ModalaService.transformTuple)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      val idMap = ids.map(x => x.id -> x).toMap
      val trajectory = setup.rootHierarchy
      val root = tryWithOption(idMap(trajectory).asInstanceOf[HierarchyRoot]).getOrElse(HierarchyRoot("empty"))
      val opsNChildren = getOperationsAndItsChildren(ids, root)
      val robotTrajectories = opsNChildren.map(_._2.filter(
        item => item.isInstanceOf[Operation] && item.attributes.getAs[List[Pose]]("poses").isDefined).
        map(_.asInstanceOf[Operation])).toList
      val makeSpan = getMakeSpan(robotTrajectories.flatten)
      val updateOps = matchGantt(robotTrajectories)

      val req = createReq(updateOps)
      println("json:")
      println(req.toJson)

      //val newTraj = makeNewTrajectory(updateOps)
      //replyTo ! Response(newTraj, SPAttributes("info"->"A trajectory from gantt"), service, reqID)


      amqProducer ! req.toJson
    }
    case DalaResponse(message,caller,Request(s,a,i,r),progress) => {
      import org.json4s._
      import org.json4s.native.JsonMethods._
      val json = parse(message)
      caller ! Response(List(), SPAttributes("final result" -> json), s, r)
      sender() ! PoisonPill // don't need consumer any more
      progress ! PoisonPill
      self ! PoisonPill
    }
    case _ => sender ! SPError("Ill formed request");
  }


  case class Precision(val p:Double)
  implicit class withAlmostEquals(d:Double) {
    def ~=(d2:Double)(implicit p:Precision) = (d-d2).abs <= p.p
  }
  implicit val precision = Precision(0.001)

  def createReq(xs: List[List[Operation]]) = {

    val poses = xs.map{ops =>
      val temp = ops.flatMap(op => op.attributes.getAs[List[Pose]]("poses")).flatten.
      foldLeft(List[Pose]()){(list, p) => list match {
        case x :: xs if x.time == p.time=> list
        case _ => p :: list 
      }}.sortWith(_.time < _.time)
      temp ++ List(temp.last.copy(time = temp.last.time+0.012),
        temp.last.copy(time = temp.last.time+0.024),
        temp.last.copy(time = temp.last.time+0.036))
    }
    val markPoses = xs.map{ops =>
      ops.flatMap(op => op.attributes.getAs[ZoneMark]("mark").map { x =>
        val ps = op.attributes.getAs[List[Pose]]("poses").get
        x.copy(start = ps.head.time, end = ps.last.time)
      })
    }.zipWithIndex.flatMap{case (list, i) => list.map(z => (i, z))}
    val times = poses.map(_.map(_.time))
    val trajectory = poses.map(_.map(_.joints))

    val makeSpan = times.flatten.max

    val robots = times zip trajectory map {case (time, traj) =>
      SPAttributes(
        "makespan" -> makeSpan,
        "samplingRate" -> 0.012,
        "timeToleranceMax" -> 1.0,
        "timeToleranceMin" -> 0.001,
        "epsilonT" -> 0.0001,
        "costScaleFactor" -> 0.001,
        "velocityLimit" -> (1 to 6).map(x => 200),
        "accelerationLimit" -> (1 to 6).map(x => 4000),
        "jerkLimit" -> (1 to 6).map(x => 15000),
        "weights" -> List(List(24, 20, 16, 12, 8, 4)),
        "time" -> time,
        "trajectory" -> traj
      )
    }

    //times


    val marks = markPoses.map{case (i, zone) =>
      val timeIndex = times(i).zipWithIndex
      println("start:"+zone.start)
      println("end:"+zone.end)
      val zStartPose = timeIndex.find(_._1 ~= zone.start).get._2
      val zEndPose = timeIndex.find(_._1 ~= zone.end).get._2
      zone.name -> Mark(i, zStartPose, zEndPose)
    }.groupBy(_._1).map{case (zName, list) =>
      list.sortWith(_._2.entersAtSample < _._2.entersAtSample).map(_._2)
    }


    SPAttributes(
      "robots" -> robots,
      "sharedZones" -> marks,
      "preservedZones" -> List()
    )
  }




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
        val newMark = x.copy(start = poses.head.time, end = poses.last.time)
        Thing(x.name, SPAttributes("mark" -> newMark))
      })
    }
    val newOps = poses.map(ps => Operation("new"+ps.head.time, List(), SPAttributes("poses"->ps)))
    val newThings = poses.map(ps => Thing("new"+ps.head.time))
    val zip = (newOps zip markOps) zip newThings
    val chs = zip.map(z => HierarchyNode(z._2.id, List(HierarchyNode(z._1._1.id, z._1._2.map(t => HierarchyNode(t.id))))))
    List(HierarchyRoot("updated from Gantt", chs)) ++ newOps ++ newThings ++ markOps.flatten
  }


}

case class StartNEnd(start: Double, end: Double)
