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
import sp.services.sopmaker.MakeASop

import org.json4s._
import scala.annotation.tailrec

case class RobotScheduleSetup(selectedSchedules: List[ID])

import oscar.cp._

class RobotOptimization(ops: List[Operation], precedences: List[(ID,ID)],
  mutexes: List[(ID,ID)], forceEndTimes: List[(ID,ID)]) extends CPModel with MakeASop {
  val timeFactor = 100.0
  def test = {
    val d = ops.map(o=>(o.attributes.getAs[Double]("duration").getOrElse(0.0) * timeFactor).round.toInt).toArray
    val indexMap = ops.map(_.id).zipWithIndex.toMap
    val numOps = ops.size
    val totalDuration = d.sum

    // start times, end times, makespan
    var s = Array.fill(numOps)(CPIntVar(0, totalDuration))
    var e = Array.fill(numOps)(CPIntVar(0, totalDuration))
    var m = CPIntVar(0 to totalDuration)

    var extra = Array.fill(mutexes.size)(CPBoolVar())
    forceEndTimes.foreach { case (t1,t2) => add(e(indexMap(t1)) == s(indexMap(t2))) }
    precedences.foreach { case (t1,t2) => add(e(indexMap(t1)) <= s(indexMap(t2))) }
    mutexes.zip(extra).foreach { case ((t1,t2),ext) =>
      val leq1 = e(indexMap(t1)) <== s(indexMap(t2))
      val leq2 = e(indexMap(t2)) <== s(indexMap(t1))
      add(leq1 || leq2)

      // extra
      add(!ext ==> leq1)
      add(leq1 ==> !ext)
      add(ext ==> leq2)
      add(leq2 ==> ext)
    }

    ops.foreach { op =>
      // except for time 0, operations can only start when something finishes
      // must exist a better way to write this
      add(e(indexMap(op.id)) == s(indexMap(op.id)) + d(indexMap(op.id)))
      val c = CPIntVar(0, numOps)
      add(countEq(c, e, s(indexMap(op.id))))
      // NOTE: only works when all tasks have a duration>0
      add(s(indexMap(op.id)) === 0 || (c >>= 0))
    }
    add(maximum(e, m))

    minimize(m)

    search(binaryFirstFail(extra++s++Array(m)))

    var sols = Map[Int, Int]()
    var ss = Map[Int,List[(ID,Int,Int)]]()
    onSolution {
      sols += m.value -> (sols.get(m.value).getOrElse(0) + 1)
      println("Makespan: " + m.value)
      println("Start times: ")
      ops.foreach { op =>
        println(op.name + ": " + s(indexMap(op.id)).value + " - " +
          d(indexMap(op.id)) + " --> " + e(indexMap(op.id)).value)
      }
      sols.foreach { case (k,v) => println(k + ": " + v + " solutions") }
      val ns = ops.map { op => (op.id, s(indexMap(op.id)).value,e(indexMap(op.id)).value) }
      ss += m.value->ns
    }

    val stats = start(timeLimit = 120) // (nSols =1, timeLimit = 60)
    println("===== oscar stats =====\n" + stats)
    val sops = ss.map { case (makespan, xs) =>
      val start = xs.map(x=>(x._1,x._2)).toMap
      val finish = xs.map(x=>(x._1,x._3)).toMap
      def rel(op1: ID,op2: ID): SOP = {
        if(finish(op1) <= start(op2))
          Sequence(op1,op2)
        else if(finish(op2) <= start(op1))
          Sequence(op2,op1)
        else
          Parallel(op1,op2)
      }

      val pairs = (for {
        op1 <- ops
        op2 <- ops if(op1 != op2)
          } yield Set(op1.id,op2.id)).toSet

      val rels = pairs.map { x => (x -> rel(x.toList(0),x.toList(1))) }.toMap

      val opsPerRob = ops.groupBy(_.attributes.getAs[String]("robotSchedule")).collect {
        case (Some(s), op) => s -> op
      }.map { case (k,v) => println("schedule " + k + " contains " + v.map(x=>x.name+" "+x.id).mkString(", "))
          v.map(_.id) }.toList
      val sop = opsPerRob.map(l=>makeTheSop(l, rels, EmptySOP)).flatten

      (makespan/timeFactor, sop, xs.map(x=>(x._1,x._2/timeFactor,x._3/timeFactor)))
    }
    (stats.completed, stats.time, sops.toList)
  }
}


object VolvoRobotSchedule extends SPService {
  val specification = SPAttributes(
    "command" -> KeyDefinition("String", List(), None),
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Create a model based on a number of robot schedules with shared zones."
    ),
    "setup" -> SPAttributes(
      "selectedSchedules" -> KeyDefinition("List[ID]", List(), Some(SPValue(List())))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[RobotScheduleSetup]("setup")),
    TransformValue("command", _.getAs[String]("command"))
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

      val setup = transform(VolvoRobotSchedule.transformTuple._1)
      val command = transform(VolvoRobotSchedule.transformTuple._2)
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

      def cleanName(str: String): String = {
        val s = if(!str.startsWith("''  -  '")) str else {
          val ns = str.substring(8,str.length)
          val p = ns.indexOf("'")
          if(p == -1) ns else ns.substring(0,p)
        }

        val pos = s.indexOf(";")
        if(pos < 0) s else s.substring(0,pos)
      }

      val robotOpIdMap = schedules.map(s=>s->hierarchyRoot.children.map(findParent(s.id,_)).flatMap(x=>x).head.item).toMap
      val robotOpMap = robotOpIdMap.flatMap{case (k,v)=>
        val x = ids.find(_.id==v)
        x match {
          case None => None
          case Some(idable) => Some(k->idable)
        }}.toMap

      def splitIntoOpsAndZones(zoneMap: Map[Operation, Set[String]],
        opList: List[Operation], activeZones: Set[String], cmds : List[String],
        availableOps: List[Operation], robotSchedule: String): (Map[Operation, Set[String]],List[Operation]) = {
        cmds match {
          case Nil => (zoneMap, opList)
          case x::xs if x.startsWith("WaitSignal AllocateZone") =>
            val zoneIndex = x.indexOf("Zone")
            val zoneStr = cleanName(x.substring(zoneIndex))
            splitIntoOpsAndZones(zoneMap, opList, activeZones + zoneStr, xs, availableOps, robotSchedule)
          case x::xs if x.startsWith("WaitSignal ReleaseZone") =>
            val zoneIndex = x.indexOf("Zone")
            val zoneStr = cleanName(x.substring(zoneIndex))
            splitIntoOpsAndZones(zoneMap, opList, activeZones - zoneStr, xs, availableOps, robotSchedule)
          case x::xs if x.startsWith("!") => splitIntoOpsAndZones(zoneMap, opList, activeZones, xs, availableOps, robotSchedule)
          case x::xs =>
            val cleanOpName = cleanName(x)
            availableOps.find(o=>o.name == cleanOpName) match {
              case Some(o) =>
                // operation o needs the active zones
                val newOp = o.copy(name = robotSchedule+"_"+o.name, attributes = o.attributes merge
                  SPAttributes("robotSchedule"->robotSchedule,"original" -> o.id))
                splitIntoOpsAndZones(zoneMap + (newOp -> activeZones), opList :+ newOp, activeZones, xs, availableOps, robotSchedule)
              case None =>
                println("skipping command " + cleanOpName + " - no matching operation")
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
        val rs = robotOpMap(op).name
        collector.v(robotScheduleVariable(rs), idleValue = Some(idle), attributes = h)
        splitIntoOpsAndZones(Map(), List(), Set(), robcmds, pops, rs)
      }

      val zoneMap = zoneMapsAndOps.foldLeft(Map():Map[Operation,Set[String]])(_++_._1)

      // hack for cp solver
      var operations: List[Operation] = List()
      var precedences: List[(ID,ID)] = List()
      var mutexes: List[(ID,ID)] = List()
      var forceEndTimes: List[(ID,ID)] = List()

      // create ops
      zoneMapsAndOps.foreach { x =>
        val ops = x._2
        operations ++= ops
        if(ops.size > 1) {
          val np = ops zip ops.tail
          precedences ++= np.map{case (o1,o2) => (o1.id,o2.id) }
          val fe = np.filter{case(o1,o2)=>zoneMap(o1) == zoneMap(o2)}.map{case (o1,o2) => (o1.id,o2.id) }
          forceEndTimes ++= fe
        }

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

        if(forbiddenPairs.nonEmpty) {
          val forbiddenStr = forbiddenPairs.map{ s =>
            val rs1 = robotScheduleVariable(s.toList(0)._1)
            val o1 = s.toList(0)._2
            val rs2 = robotScheduleVariable(s.toList(1)._1)
            val o2 = s.toList(1)._2
            mutexes:+=(o1.id,o2.id)
            s"(${rs1} == ${o1.name} && ${rs2} == ${o2.name})" }.mkString(" || ")
          collector.x(zone, Set(forbiddenStr), attributes=h)
        }
      }
      mutexes = mutexes.distinct

      import CollectorModelImplicits._
      val uids = collector.parseToIDables()

      // fix up oscar problem ids
      operations = uids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])

      def getNewID(oldID: ID) = {
        (for {
          newop <- operations
          origid <- newop.attributes.getAs[ID]("original")
          if origid == oldID
        } yield {
          newop.id
        }).head
      }


      mutexes = mutexes.map{case (x,y) => (getNewID(x), getNewID(y)) }
      precedences = precedences.map{case (x,y) => (getNewID(x), getNewID(y)) }
      forceEndTimes = forceEndTimes.map{case (x,y) => (getNewID(x), getNewID(y)) }

      val hids = uids ++ addHierarchies(uids, "hierarchy")

      val ro = new RobotOptimization(operations, precedences, mutexes, forceEndTimes)
      val roFuture = Future { ro.test }

      // now, extend model and run synthesis
      for {
        Response(ids,_,_,_) <- askAService(Request("ExtendIDablesBasedOnAttributes",
          SPAttributes("core" -> ServiceHandlerAttributes(model = None,
            responseToModel = false,onlyResponse = true, includeIDAbles = List())),
          hids, ID.newID), sh)

        ids_merged = hids.filter(x=> !ids.exists(y=>y.id==x.id)) ++ ids

        Response(ids2,synthAttr,_,_) <- askAService(Request("SynthesizeModelBasedOnAttributes",
          SPAttributes("core" -> ServiceHandlerAttributes(model = None,
            responseToModel = false, onlyResponse = true, includeIDAbles = List())),
          ids_merged, ID.newID), sh)

        numstates = synthAttr.getAs[Int]("nbrOfStatesInSupervisor").getOrElse("-1")
        bddName = synthAttr.getAs[String]("moduleName").getOrElse("")

        ids_merged2 = ids_merged.filter(x=> !ids2.exists(y=>y.id==x.id)) ++ ids2

        (cpCompl,cpTime,cpSols) <- roFuture
        sops = cpSols.map { case (makespan,sop,gantt) =>
          (makespan,SOPSpec(s"path_${makespan}", sop), gantt)
        }.sortBy(_._1)
      } yield {
        val resAttr = SPAttributes("numStates"-> numstates, "cpCompleted" -> cpCompl, "cpTime" -> cpTime, "cpSops" -> sops, "bddName" -> bddName)
        replyTo ! Response(ids_merged2 ++ sops.map(_._2), resAttr, rnr.req.service, rnr.req.reqID)
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
