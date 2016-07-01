package sp.virtcom

import akka.actor._
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent.duration._
import sp.services.AddHierarchies

import org.json4s._
import scala.annotation.tailrec

case class RobotScheduleSetup(selectedSchedules: List[ID])

object VolvoRobotSchedule extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Create a model based on a number of robot schedules with shared zones."
    ),
    "setup" -> SPAttributes(
      "selectedSchedules" -> KeyDefinition("List[ID]", List(), Some(SPValue(List())))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[RobotScheduleSetup]("setup"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(sh: ActorRef) = ServiceLauncher.props(Props(classOf[VolvoRobotSchedule], sh))
}

class VolvoRobotSchedule(sh: ActorRef) extends Actor with ServiceSupport with AddHierarchies {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)
      progress ! SPAttributes("progress" -> "starting volvo robot schedule")

      val setup = transform(VolvoRobotSchedule.transformTuple)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
      val schedules = ops.filter(op => setup.selectedSchedules.contains(op.id))
      // todo: find the correct hierarchy root
      val hierarchyRoot = ids.filter(_.isInstanceOf[HierarchyRoot]).map(_.asInstanceOf[HierarchyRoot]).head

      def findParent(id: ID, node: HierarchyNode): Option[HierarchyNode] = {
        if(node.children.exists(_.item == id)) Some(node)
        else {
          val res = node.children.map(findParent(id,_)).flatMap(x=>x)
          if(res.isEmpty) None
          else Some(res.head)
        }
      }

      def opsAtLevel(node: HierarchyNode): List[Operation] = {
        ops.filter(o=>node.children.exists(c=>c.item == o.id))
      }

      def dropSemiColon(str: String): String = {
        val pos = str.indexOf(";")
        str.substring(0,pos)
      }

      def splitIntoOpsAndZones(zoneMap: Map[Operation, Set[String]],
        opList: List[Operation], activeZones: Set[String], cmds : List[String],
        availableOps: List[Operation], robotSchedule: String): (Map[Operation, Set[String]],List[Operation]) = {
        cmds match {
          case Nil => (zoneMap, opList)
          case x::xs if x.startsWith("WaitSignal AllocateZone") =>
            val zoneIndex = x.indexOf("Zone")
            val zoneStr = dropSemiColon(x.substring(zoneIndex))
            splitIntoOpsAndZones(zoneMap, opList, activeZones + zoneStr, xs, availableOps, robotSchedule)
          case x::xs if x.startsWith("WaitSignal ReleaseZone") =>
            val zoneIndex = x.indexOf("Zone")
            val zoneStr = dropSemiColon(x.substring(zoneIndex))
            splitIntoOpsAndZones(zoneMap, opList, activeZones - zoneStr, xs, availableOps, robotSchedule)
          case x::xs if x.startsWith("!") => splitIntoOpsAndZones(zoneMap, opList, activeZones, xs, availableOps, robotSchedule)
          case x::xs =>
            val withoutSemiColon = dropSemiColon(x)
            availableOps.find(o=>o.name == withoutSemiColon) match {
              case Some(o) =>
                // operation o needs the active zones
                val newOp = o.copy(name = robotSchedule+"_"+o.name, attributes = o.attributes merge SPAttributes("robotSchedule"->robotSchedule))
                splitIntoOpsAndZones(zoneMap + (newOp -> activeZones), opList :+ newOp, activeZones, xs, availableOps, robotSchedule)
              case None =>
                println("skipping command " + withoutSemiColon + " - no matching operation")
                splitIntoOpsAndZones(zoneMap, opList, activeZones, xs, availableOps, robotSchedule)
            }
        }
      }

      // create variables, ops and zones
      // use some sweet hidden mutability, scala style
      val h = SPAttributes("hierarchy" -> Set("VRS_"+schedules.map(_.name).toSet.mkString("_")))
      case class VolvoRobotScheduleCollector(val modelName: String = "VolvoRobotSchedule") extends CollectorModel
      val collector = VolvoRobotScheduleCollector()

      def robotScheduleVariable(rs: String) = "v"+rs+"_pos"
      val idle = "idle"

      val zoneMapsAndOps = schedules.zipWithIndex.map { case (op,i) =>
        // find the right level among the hierarchy nodes
        val p = hierarchyRoot.children.map(findParent(op.id,_)).flatMap(x=>x).head
        val pops = opsAtLevel(p)
        println("schedule " + op.name + " contains ops " + pops.map(_.name).mkString(","))
        val robcmds = op.attributes.getAs[List[String]]("robotcommands").getOrElse(List())
        val rs = s"RS${i+1}"
        collector.v(robotScheduleVariable(rs), idleValue = Some(idle), attributes = h)
        splitIntoOpsAndZones(Map(), List(), Set(), robcmds, pops, rs)
      }

      val zoneMap = zoneMapsAndOps.foldLeft(Map():Map[Operation,Set[String]])(_++_._1)

      // create ops
      zoneMapsAndOps.foreach { x =>
        val ops = x._2
        ops.foreach { op =>
          println("Operation " + op.name + " in zones: " + zoneMap(op).mkString(","))
        }
        ops.foldLeft(idle){case (s,o)=>{
          val done = if(ops.reverse.head == o) idle else o.name + "_done" // go back to init
          val rs = o.attributes.getAs[String]("robotSchedule").getOrElse("error")
          val trans = SPAttributes(collector.aResourceTrans(robotScheduleVariable(rs), s, o.name, done))
          collector.op(o.name, Seq(o.attributes merge trans merge h))
          done
        }}
      }

      // create forbidden zones
      val zones = zoneMap.map { case (o,zones) => zones }.flatten.toSet
      zones.foreach { zone =>
        val opsInZone = zoneMap.filter { case (o,zones) => zones.contains(zone) }.map(_._1)
        val forbiddenPairs = (for {
          o1 <- opsInZone
          o2 <- opsInZone if o1 != o2
          rs1 <- o1.attributes.getAs[String]("robotSchedule")
          rs2 <- o2.attributes.getAs[String]("robotSchedule") if rs1 != rs2
        } yield {
          Set((rs1,o1),(rs2,o2))
        }).toSet
        
        val forbiddenStr = forbiddenPairs.map{ s =>
          val rs1 = robotScheduleVariable(s.toList(0)._1)
          val o1 = s.toList(0)._2
          val rs2 = robotScheduleVariable(s.toList(1)._1)
          val o2 = s.toList(1)._2
          s"(${rs1} == ${o1.name} && ${rs2} == ${o2.name})" }.mkString(" || ")
        collector.x(zone, Set(forbiddenStr), attributes=h)
      }

      import CollectorModelImplicits._
      val uids = collector.parseToIDables()
      val hids = uids ++ addHierarchies(uids, "hierarchy")

      // now, extend model and run synthesis
      for {
        Response(ids,_,_,_) <- askAService(Request("ExtendIDablesBasedOnAttributes",
          SPAttributes("core" -> ServiceHandlerAttributes(model = None,
            responseToModel = false,onlyResponse = true, includeIDAbles = List())),
          hids, ID.newID), sh)

        ids_merged = hids.filter(x=> !ids.exists(y=>y.id==x.id)) ++ ids

        Response(ids2,_,_,_) <- askAService(Request("SynthesizeModelBasedOnAttributes",
          SPAttributes("core" -> ServiceHandlerAttributes(model = None,
            responseToModel = false, onlyResponse = true, includeIDAbles = List())),
          ids_merged, ID.newID), sh)

        ids_merged2 = ids_merged.filter(x=> !ids2.exists(y=>y.id==x.id)) ++ ids2

        Response(shortest,_,_,_) <- askAService(Request("SimpleShortestPath",
          SPAttributes("core" -> ServiceHandlerAttributes(model = None,
            responseToModel = false,onlyResponse = true, includeIDAbles = List()),
            "parameters" -> SPAttributes("waitAllowed" -> true, "longest" -> false)),
          ids_merged2, ID.newID), sh)
        Response(longest,_,_,_) <- askAService(Request("SimpleShortestPath",
          SPAttributes("core" -> ServiceHandlerAttributes(model = None,
            responseToModel = false,onlyResponse = true, includeIDAbles = List()),
            "parameters" -> SPAttributes("waitAllowed" -> false, "longest" -> true)),
          ids_merged2, ID.newID), sh)        
      } yield {
        replyTo ! Response(ids_merged2 ++ shortest ++ longest, SPAttributes(), rnr.req.service, rnr.req.reqID)
        terminate(progress)
      }
    }
    case _ => sender ! SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }

}
