// Import stuff and setup the RobotScheduleSetup class
package sp.virtcom

import java.util.UUID

import akka.actor._
import sp.system._
import sp.system.messages.{KeyDefinition, TransformValue, _}
import sp.domain.{Alternative, Arbitrary, SPAttributes, Sequence, _}
import sp.domain.Logic._

import scala.collection.mutable.LinkedHashMap
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


// Constraint programming (CP):
class RobotOptimization(ops: List[Operation], precedences: List[(ID,ID)],
                        mutexes: List[(ID,ID)], forceEndTimes: List[(ID,ID)]) extends CPModel with MakeASop {

  val timeFactor = 100.0
  def test = {
    val duration = ops.map(o=>(o.attributes.getAs[Double]("duration").getOrElse(0.0) * timeFactor).round.toInt).toArray
    val indexMap = ops.map(_.id).zipWithIndex.toMap
    val numOps = ops.size
    val totalDuration = duration.sum

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
      add(e(indexMap(op.id)) == s(indexMap(op.id)) + duration(indexMap(op.id)))
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
/*
      ops.foreach { op =>
        println(op.name + ": " + s(indexMap(op.id)).value + " - " +
         duration(indexMap(op.id)) + " --> " + e(indexMap(op.id)).value)
      } */
     // sols.foreach { case (k,v) => println(k + ": " + v + " solutions") }
      val ns = ops.map { op => (op.id, s(indexMap(op.id)).value,e(indexMap(op.id)).value) }
      ss += m.value->ns
    }

    val stats = start(timeLimit = 120) // (nSols =1, timeLimit = 60)

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
      }.map { case (k,v) => //println("schedule " + k + " contains " + v.map(x=>x.name+" "+x.id).mkString(", "))
        v.map(_.id) }.toList

      val sop = opsPerRob.map(l=>makeTheSop(l, rels, EmptySOP)).flatten

      (makespan/timeFactor, sop, xs.map(x=>(x._1,x._2/timeFactor,x._3/timeFactor)))
    }
    (stats.completed, stats.time, sops.toList)
  }
}

// Specifies what type of object VolvoRobotSchedule will receive
object VolvoRobotSchedule extends SPService {
  val specification = SPAttributes(
    "command" -> KeyDefinition("String", List(), None),
    "SopID"   -> KeyDefinition("String", List(), None),
    "neglectedCases" -> KeyDefinition("Map", List(), None),
    "checkedSOP"  -> KeyDefinition("Bool", List(), None),
    "checkedTime" -> KeyDefinition("Bool", List(), None),
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
    TransformValue("command", _.getAs[String]("command")),
    TransformValue("SopID", _.getAs[String]("SopID")),
    TransformValue("neglectedCases", _.getAs[Map[String,List[Operation]]]("neglectedCases")),
    TransformValue("checkedSOP", _.getAs[Boolean]("checkedSOP")),
    TransformValue("checkedTime", _.getAs[Boolean]("checkedTime"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(sh: ActorRef) = ServiceLauncher.props(Props(classOf[VolvoRobotSchedule], sh))
}


class VolvoRobotSchedule(sh: ActorRef) extends Actor with ServiceSupport with AddHierarchies with MakeASop{
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher


  def receive = { // Receive message from gui with selected schedules, their properties and ids as well as the reqID of this instance.
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val progress = context.actorOf(progressHandler) // these progress messages are required to be initialized and sent even tho they are not used much here.
      progress ! SPAttributes("progress" -> "starting volvo robot schedule")

      val setup = transform(VolvoRobotSchedule.transformTuple._1) // Get the setup
      val command = transform(VolvoRobotSchedule.transformTuple._2) // and command information from the message

      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation]) // filter out the operations from the ids
      val schedules = ops.filter(op => setup.selectedSchedules.contains(op.id)) // Get the selected operations as a list

      // todo: find the correct hierarchy root
      val hierarchyRoots = ids.filter(_.isInstanceOf[HierarchyRoot]).map(_.asInstanceOf[HierarchyRoot])

      var robotOpIdMapN = Map[Operation, ID]()

      hierarchyRoots.foreach(hRoot => { schedules.foreach(s=>
      {  if(! hRoot.children.map(findParent(s.id,_)).flatMap(x=>x).isEmpty)
      {  robotOpIdMapN += s->hRoot.children.map(findParent(s.id,_)).flatMap(x=>x).head.item}})})

      val robotOpMap = robotOpIdMapN.flatMap{case (k,v)=> // map operations to Idables
        val x = ids.find(_.id==v)
        x match {
          case None => None
          case Some(idable) => Some(k->idable)
        }}.toMap
      val scheduleNames = schedules.map(op => {if(op.attributes.getAs[List[String]]("robotcommands").getOrElse(List()).nonEmpty) op.name else robotOpMap(op).name}).toSet.mkString("_") // Creat Schedule names, for robots schedules and other resources.

      case class VolvoRobotScheduleCollector(val modelName: String = "VolvoRobotSchedule") extends CollectorModel
      val collector = VolvoRobotScheduleCollector() // This can collect all of the operations and create transition variables between them
      val idle = "idle" // This is a value for the variables created in the collector, and it should later also be assigned to the first operation(s) in a schedule (SOP)
      var uids = List[IDAble with Product with Serializable]() // The type of data that will come out of the collector


      // Create zone and operation SOPs for the selected robot Schedules and resources.
      if(command == "GenerateSops"){
        val h = SPAttributes("hierarchy" -> Set("VRS_"+ scheduleNames ))  // This will be the name of the new hierarchy
        val hierarchyRoot = hierarchyRoots.head

        val zoneMapsAndOps = schedules.map { schedule => // go through each schedule ( selected op)

          val robcmds = schedule.attributes.getAs[List[String]]("robotcommands").getOrElse(List()) // Gets all of the robotcommands for the selected operation/schedule

          var p = hierarchyRoot.children.map(findParent(schedule.id,_)).flatMap(x=>x).head // find the right level among the hierarchy nodes
          var rs = robotOpMap(schedule).name   // The name of the Selected operation/schedule

          if(robcmds.isEmpty){ // If this is another resource i.e not a robot
            p = p.children.filter(node => node.item == schedule.id).head
            rs = "LD" + schedule.name  // Todo: There seems to be a problem in the synthesis with handling names starting with numbers... and also "-" characters, havn't found the origin of the error, which is why I add this: "LD".
          }

          val pops = opsAtLevel(p,ops) // Gets the topmost operation names of the selected hirarchy

          collector.v(robotScheduleVariable(rs), idleValue = Some(idle), attributes = h) //  Update the collector to gather all of the variables and operations from the functions below

          if(robcmds.nonEmpty)
            addRobot(Map[String, List[List[Operation]]](), Map[String, Seq[SOP]](), Set[String](), robcmds, pops, rs, hierarchyRoot, ops, collector, h) // Get Sops, Ops and zones from robot schedule
          else{
            val rSOP= addOtherResource(pops: List[Operation], rs: String, h: SPAttributes, collector: CollectorModel)
            (Map[String, List[List[Operation]]](),Set[String](), List(rSOP), Map[String, Seq[SOP]]()) // Just so that the result will conform with the addRobot function
          }
        }
        var operations = getOpsFromCollector(collector) // Get all operations from the collector

        val sops = zoneMapsAndOps.map { x => x._3 }.flatten // extract SOPS
        val sopspec = SOPSpec(schedules.map(_.name).toSet.mkString("_") + "_Original_SOP", sops, h) // Create a Sop Spec with (name, sops, belonging to hierarchy)

        var allZoneSopMapsMerged =  Map[String, Seq[SOP]]() // Merges all of the key values in the zone sop maps
        zoneMapsAndOps.map { zoneSopMap => zoneSopMap._4.map { case (z, sopSeq) =>  if(allZoneSopMapsMerged.contains(z)) allZoneSopMapsMerged += z-> (allZoneSopMapsMerged(z) ++  sopSeq )   else allZoneSopMapsMerged += z-> sopSeq }}

        var zonespecs = allZoneSopMapsMerged.map { case (z, l) => SOPSpec(z, List((Arbitrary(SOP()) <-- l)), h) } // Creates arbitrary SOPs for each Zone and SopSpecs for them

        val nids = List(sopspec) ++ zonespecs ++ operations // new ids, i.e everything that we want to return
        val hids = nids ++ addHierarchies(nids, "hierarchy") // hierarchy ids, everything that we want to return and a bit more to actually add the Hierarchy to the SP tree.

        // Create a response message and send it on the bus "back to the GUI"
        replyTo ! Response(hids,SPAttributes(), rnr.req.service, rnr.req.reqID)
        terminate(progress) // done
      }

      // Reads the previously created SOPs and sends all of the alternative cases to the Gui
      else if( command == "loadCases"){

        val SopID = transform(VolvoRobotSchedule.transformTuple._3) // Get the SOP ID from the message, sent from the GUI
        var parentNodeToSoplist = List[HierarchyNode]()
        var parentroot = List[HierarchyRoot]() // find the right hierarchyRoot and node
        hierarchyRoots.foreach(hRoot => {hRoot.children.foreach(child => {if(child.item.toString == SopID){ parentroot = List(hRoot)}}) })
        var parentNodeToSop =parentroot.head

        val allNodesInHierarchy = ids.filter(o=>parentNodeToSop.children.exists(c=>c.item == o.id)) // Get all of the Hierarchy children as idable objects from ids
        val allSopSpecs = allNodesInHierarchy.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec])  // filter out the Sop Specs
        var sopSpec = allSopSpecs.find(c=> c.id.toString == SopID).get //Get the SOPSpec with the Right ID.

        var caseMap = Map[String,List[Operation]]()
        sopSpec.sop.foreach(individualSop => {
          getCasesFromSop(individualSop, false)
        })

        def getCasesFromSop(individualSop :SOP, alt :Boolean) : List[Operation] ={ // Gets the cases as operations out of the SOP
          var caseList = List[Operation]()
          var alternative = alt

          if(individualSop.isInstanceOf[Sequence]) {
            individualSop.sop.foreach(sopNode => {
              caseList ++= getCasesFromSop(sopNode, alternative)// call the function again with the sopNode Sop
              alternative = false // I only want the first operation of the case sequence.
            })
          }

          else if(individualSop.isInstanceOf[Hierarchy]){
            var op = ops.filter(op => op.id == individualSop.asInstanceOf[Hierarchy].operation).head // get the ID from the node, find the corresponding operation
            if(alternative){caseList :+= op}
          }

          else if(individualSop.isInstanceOf[Alternative]){
            individualSop.sop.foreach(sopNode => {
              caseList ++= getCasesFromSop(sopNode, true) // call the function again with the sopNode Sop
            })
            if(caseList.nonEmpty) {
              val robotSchedule = caseList.head.attributes.getAs[String]("robotSchedule").getOrElse("error") // get robot schedule name.
              val caseName = caseList.head.name.substring(robotSchedule.length + 1, caseList.head.name.length).replaceAll("[^a-zA-Z]+", "")
              caseMap += ((robotSchedule + "_" + caseName) -> caseList)
            }
            caseList = List[Operation]()
          }
          caseList
        }

        val resAttr = SPAttributes("caseMap" -> caseMap)
        replyTo ! Response(addHierarchies(List[Operation](), "hierarchy"),resAttr, rnr.req.service, rnr.req.reqID)
        terminate(progress)
      }

      // 1. Reads the SOPs. 2. Creates new SOPs and operations with new IDs. 3. Given the selected Alternatives, zones and SOPs solves using CP. 4. Creates a Supervisor.
      else if(command == "calculateUsingSops") {

        val SopID = transform(VolvoRobotSchedule.transformTuple._3) // Get the SOP ID from the message, sent from the GUI
        val neglectedCases = transform(VolvoRobotSchedule.transformTuple._4) // Get cases that should not be a part of the solution
        val checkedSOP = transform(VolvoRobotSchedule.transformTuple._5)  // If you want to send back the original SOP but with added conditions for transitions,
        val checkedTime = transform(VolvoRobotSchedule.transformTuple._6) // Will add a duration of 1 to the operations that have none

        val neglectedOpIds = neglectedCases.values.flatten.toList.map(op => op.id) // Gets the IDs of the neglected cases

        val h = SPAttributes("hierarchy" -> Set("VRS_"+ scheduleNames)) // hierarchy name

        var parentNodeToSoplist = List[HierarchyNode]()
        var parentroot = List[HierarchyRoot]()
        hierarchyRoots.foreach(hRoot => {
          hRoot.children.foreach(child => {if(child.item.toString == SopID){ parentroot = List(hRoot)}}) // hierarchyroot or node...
        })
        var parentNodeToSop =parentroot.head

        val allNodesInHierarchy = ids.filter(o=>parentNodeToSop.children.exists(c=>c.item == o.id)) // Get all of the Hierarchy children as idable objects from ids
        val allSopSpecs = allNodesInHierarchy.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec])  // filter out the Sop Specs
        var sopSpec = allSopSpecs.find(c=> c.id.toString == SopID).get //Get the SOPSpec with the Right ID. ( the one from the generate SOPS)
        var zonespecs = if(allSopSpecs.size >1) allSopSpecs.diff(List(sopSpec)).toIterable  else Iterable[SOPSpec]()// Remove the main sopspec from the list, remaining are the zone/station sopspecs
        val allOpsInHierarchy = allNodesInHierarchy.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])  // filter out the Operations

        val collector2 = VolvoRobotScheduleCollector()
        // Go through the list of (operations) get their schedule names and create Collector variables.
       allOpsInHierarchy.map{op => op.attributes.getAs[String]("robotSchedule").getOrElse("error")}.distinct.foreach{s => {collector.v(s, idleValue = Some(idle), attributes = h) ; collector2.v(s, idleValue = Some(idle), attributes = h)} }

        // Go through the SOPs for each resource, one by one and create straight operation sequences for the optimization.
        // Create new IDs for the operations, and new SOPs for those IDs
        var opSequences = List[List[Operation]]() // A list with lists for all possible sequences
        var ss = List[SOP]() // The SOP
        sopSpec.sop.foreach(individualSop => {
          var exOps =  extractFromSOP(individualSop :SOP, "idle", true, collector, ops, h, checkedTime)
          ss :+= exOps._3
          opSequences ++= exOps._1
        })

        sopSpec = SOPSpec(schedules.map(_.name).toSet.mkString("_")+ "_Whole", ss,h) // This is the sopspec for the "original" full SOP with new IDs

        import CollectorModelImplicits._
        uids = collector.parseToIDablesWithIDs() // extract variables, transitions and operations from collector
        var operations = uids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation]) // Get all operations from the collector


        var zoneMapping = Map[Operation,Set[String]]() // Create op zone Map
        operations.foreach(op => zoneMapping += (op -> Set())) // fill the map with all operations

        var zonespecsNew = List[SOPSpec]() // Create a new zonespec with the new operation ids and create the zone mapping

        var zoneMappingList = List[Map[Operation,Set[String]]]()
        zonespecs.foreach(zSpec=> {
          val zoneName =zSpec.name
          var zonesops = List[SOP]()
          zSpec.sop.foreach(sopNode => {
            zonesops :+= zoneSpecsAndZoneMap(sopNode, zoneName)._1
          })
          zonespecsNew :+= SOPSpec(zoneName, zonesops, h)
        })
        zonespecs = zonespecsNew.toIterable


        def zoneSpecsAndZoneMap(sopNode :SOP, zoneName: String) : (SOP, Map[Operation,Set[String]])={
          var zoneMappingNew = Map[Operation,Set[String]]()
          var newSop = SOP()
          if(sopNode.isInstanceOf[Arbitrary]) {
            var arbSeqs = Seq[SOP]()
            sopNode.sop.foreach(subSopNode => {arbSeqs :+= zoneSpecsAndZoneMap(subSopNode, zoneName)._1; zoneMappingList :+= zoneSpecsAndZoneMap(subSopNode, zoneName)._2})
            newSop = Arbitrary(SOP()) <-- arbSeqs
          }
          else if (sopNode.isInstanceOf[Sequence]){
            var seqSeqs = Seq[SOP]()
            sopNode.sop.foreach(subSopNode => {seqSeqs :+= zoneSpecsAndZoneMap(subSopNode, zoneName)._1; zoneMappingNew ++= zoneSpecsAndZoneMap(subSopNode, zoneName)._2})
            newSop = Sequence(SOP()) <-- seqSeqs
          }
          else if (sopNode.isInstanceOf[Hierarchy]){
            val op = operations.filter(op => op.attributes.getAs[String]("original").getOrElse("error") == sopNode.asInstanceOf[Hierarchy].operation.toString).head
            zoneMapping += ( op -> (zoneMapping(op) + zoneName))
            zoneMappingNew += ( op -> Set(zoneName))

            newSop = SOP() <-- Seq(Hierarchy(op.id))
          }
          (newSop, zoneMappingNew)
        } // Create a zonemap and a new zonespec with the new operation ids.





        // Solve with CP for every possible sequence of operations.
        var ganttChart = List[(ID, Double,Double)]() // A list where if the CP optimization is run in several steps could contain the result from each step
        var makespans = List[Double]() // A list where if the CP optimization is run in several steps could contain the result from each step

        val zones = zoneMapping.map { case (o, zones) => zones }.flatten.toSet // All the zone names as a set of strings
        var mutexes: List[(ID,ID)] = List() // Should contain all a list for all operations in zones (that should not execute at the same time) ex: Zone 1: op1, op2, op3 -> mutexes: (op1,op2),(op1,op3),(op2,op3)


        // Creates the mutexes, seems correct
        val zoneSeqs = zones.map { zone =>
          val opsInZone = zoneMapping.filter { case (o,zones) => zones.contains(zone) }.map(_._1)

          val forbiddenPairs = (for {
            o1 <- opsInZone
            o2 <- opsInZone if o1 != o2
          } yield {
            Set(o1,o2)
          }).toSet

          val l = (if(forbiddenPairs.nonEmpty) {

            val forbiddenOps = forbiddenPairs.map{ s =>
              val o1 = s.toList(0)
              val o2 = s.toList(1)
              mutexes:+=(o1.id,o2.id)
              Set(o1.id,o2.id)
            }.toList.flatten.distinct
            // collector.x(zone, operations=forbiddenOps, attributes=h)
            forbiddenOps
          } else List())
          zone -> l
        }.toMap

        mutexes = mutexes.distinct


        var precedences: List[(ID,ID)] = List()
        var forceEndTimes: List[(ID,ID)] = List()


        var approvedOps = List[Operation]()  // Create a list where all the wanted operations from the next step can be saved
        // going through each oplist and adding information to the precedence and forceEndTime lists
        opSequences.foreach(oplist => {

          operations = updateOpSequenceFromCollector(oplist,collector)

          var opListIds = oplist.map(op => op.attributes.getAs[ID]("original").get)

          if (opListIds.intersect(neglectedOpIds).isEmpty){

            // Creates a new Zonemap where all of the zonemap operations are contained in the current oplist
            var zoneMappingNew = Map[Operation, Set[String]]()
            operations.foreach(op => zoneMappingNew += (op -> Set()))
            zoneMappingList.foreach(zMap => {
              if (zMap.keySet subsetOf operations.toSet) {
                zMap.keys.foreach(op => zoneMappingNew += (op -> (zoneMappingNew(op) ++ zMap(op))))
              }
            })


            //Adds transition conditions to the operations
            operations.foldLeft(idle) { case (s, o) => {
              val done = if (operations.reverse.head == o) idle else o.name + "_done" // go back to init
              val rs = o.attributes.getAs[String]("robotSchedule").getOrElse("error")
              val trans = SPAttributes(collector2.aResourceTrans(rs, s, o.name, done))
              var duration =  o.attributes.getAs[Double]("duration").getOrElse(0.0)
              val spAttrDuration = if(duration > 0.0 || !checkedTime) SPAttributes("duration" -> duration) else SPAttributes("duration" -> 1.0)
              collector2.opWithID(o.name, Seq(o.attributes merge trans merge h merge spAttrDuration), o.id)
              done
            }
            }
            operations = updateOpSequenceFromCollector(oplist,collector2)
            approvedOps ++= operations

            var zoneMap = zoneMappingNew //zoneMapping

            // Creates a new ZoneMap for the current operation Sequence, containing only those operations.

            // makes sure that the zonemap has the right IDs
            val zoness = zoneMap.map { case (o, zoness) => zoness }.flatten.toSet
            var newZoneMap = Map[Operation, Set[String]]()
            zoneMap.map { case (o, zoness) => {
              operations.foreach({ opWithTrans => if (opWithTrans.id == o.id) newZoneMap += (opWithTrans -> zoness) })
            }
            }
            zoneMap = newZoneMap


            // For the CP solver-----------
            if (operations.size > 1) {
              val np = operations zip operations.tail
              precedences ++= np.map { case (o1, o2) => (o1.id, o2.id) } // Orders all operations after eachother in the order they were given from the op list
              if (!zoneMap.isEmpty) {
                // Checks which operations have the same zoneMap
                val fe = np.filter { case (o1, o2) => zoneMap(o1) == zoneMap(o2) }.map { case (o1, o2) => (o1.id, o2.id) } // Seems to do it like: If there are more than one operation with the same zones, then they are placed in fe.
                // Like: op1: in zones: Station1,Zone1
                //       op2: in zones: Station1,Zone1 --> fe: (op1,op2),
                forceEndTimes ++= fe
              }
            }
          }
        })

        approvedOps = approvedOps.distinct
        val approvedIds = approvedOps.map(op => op.id)

        mutexes = mutexes.distinct
        forceEndTimes = forceEndTimes.distinct
        precedences = precedences.distinct

        mutexes = mutexes.filter(m => (approvedIds.contains(m._1) && approvedIds.contains(m._2)))
        // When all of the operation sequences have been processed and the precedences + forceEndTimes have been created.
        // The mutexes which are just given by the great zonemap and all operations + precedences & forceEndTimes are sent to the robot optimization.

        val ro = new RobotOptimization(approvedOps, precedences, mutexes, forceEndTimes)
        val roFuture = Future {
          ro.test
        }


        if(!checkedSOP) {


          // Create a new ZoneSpec based on the selected cases.
          var zonespecsStraight = List[SOPSpec]() // Create a new zonespec with the new operation ids and create the zone mapping
          zonespecs.foreach(zSpec => {
            val zoneName = zSpec.name
            var zonesops = List[SOP]()
            zSpec.sop.foreach(sopNode => {
              zonesops :+= straightZoneSpecs(sopNode, zoneName)
            })
            zonespecsStraight :+= SOPSpec(zoneName, zonesops, h)
          })
          zonespecs = zonespecsStraight.toIterable

          def straightZoneSpecs(sopNode: SOP, zoneName: String): (SOP) = {
            var newSop = SOP()
            if (sopNode.isInstanceOf[Arbitrary]) {
              var arbSeqs = Seq[SOP]()
              sopNode.sop.foreach(subSopNode => {
                if (straightZoneSpecs(subSopNode, zoneName) != SOP()) arbSeqs :+= straightZoneSpecs(subSopNode, zoneName)
              })
              if (!arbSeqs.isEmpty)
                newSop = Arbitrary(SOP()) <-- arbSeqs
            }
            else if (sopNode.isInstanceOf[Sequence]) {
              var seqSeqs = Seq[SOP]()
              sopNode.sop.foreach(subSopNode => {
                if (straightZoneSpecs(subSopNode, zoneName) != SOP()) seqSeqs :+= straightZoneSpecs(subSopNode, zoneName)
              })
              if (!seqSeqs.isEmpty)
                newSop = Sequence(SOP()) <-- seqSeqs
            }
            else if (sopNode.isInstanceOf[Hierarchy]) {
              val op = approvedOps.filter(op => op.id == sopNode.asInstanceOf[Hierarchy].operation)
              if (op.nonEmpty) {
                newSop = SOP() <-- Seq(Hierarchy(op.head.id))
              }
            }
            newSop
          } // Create a zonemap and a new zonespec with the new operation ids.

          // Create a new sopSpec based on the selected cases
          var newSopSeq = SOP()

          var sopSeqs = Seq[SOP]()
          sopSpec.sop.foreach(individualSop => {
            straightSop(individualSop: SOP)
            sopSeqs :+= newSopSeq
            newSopSeq = SOP()
          })

          ss = sopSeqs.toList
          sopSpec = SOPSpec(schedules.map(_.name).toSet.mkString("_") + "_Optimized", ss, h)

          def straightSop(individualSop: SOP): (SOP) = {
            var newSop = SOP()

            if (individualSop.isInstanceOf[Sequence] || individualSop.isInstanceOf[Alternative]) {
              individualSop.sop.foreach(sopNode => {
                straightSop(sopNode) // call the function again with the sopNode Sop
              })
            }
            else if (individualSop.isInstanceOf[Hierarchy]) {
              if (approvedIds.contains(individualSop.asInstanceOf[Hierarchy].operation)) {
                newSop = SOP() <-- Seq(Hierarchy(individualSop.asInstanceOf[Hierarchy].operation))
                if (newSopSeq.isEmpty) newSopSeq = Sequence(SOP()) <-- Seq(newSop) else newSopSeq += newSop
              }
            }
            (newSop)
          }

          import CollectorModelImplicits._
          uids = collector2.parseToIDablesWithIDs()

        }


        // For the synthesis:
        var nids = List(sopSpec) ++ zonespecs ++ uids
        var hids = nids ++ addHierarchies(nids, "hierarchy")


        //--------------------------------------------------------------------------------------------------------------------------
        // now, extend model, run synthesis and get the optimization results
        for {
        // Extend model
          Response(ids, _, _, _) <- askAService(Request("ExtendIDablesBasedOnAttributes",
            SPAttributes("core" -> ServiceHandlerAttributes(model = None,
              responseToModel = false, onlyResponse = true, includeIDAbles = List())),
            hids, ID.newID), sh)

          ids_merged = hids.filter(x => !ids.exists(y => y.id == x.id)) ++ ids

          // Run synthesis with Supremica
          Response(ids2, synthAttr, _, _) <- askAService(Request("SynthesizeModelBasedOnAttributes",
            SPAttributes("core" -> ServiceHandlerAttributes(model = None,
              responseToModel = false, onlyResponse = true, includeIDAbles = List())),
            ids_merged, ID.newID), sh)

          // Get attributes from the result of the synthesis
          numstates = synthAttr.getAs[Int]("nbrOfStatesInSupervisor").getOrElse("-1")
          bddName = synthAttr.getAs[String]("moduleName").getOrElse("")

          ids_merged2 = ids_merged.filter(x => !ids2.exists(y => y.id == x.id)) ++ ids2

          //Get info about the CP solution
          (cpCompl, cpTime, cpSols) <- roFuture
          sops = cpSols.map { case (makespan, sop, gantt) =>
            (makespan, SOPSpec(s"path_${makespan}", sop), gantt)
          }.sortBy(_._1)

        } yield {
          val resAttr = SPAttributes("numStates" -> numstates, "cpCompleted" -> cpCompl, "cpTime" -> cpTime, "cpSops" -> sops, "bddName" -> bddName)
          replyTo ! Response(ids_merged2 ++ sops.map(_._2), resAttr, rnr.req.service, rnr.req.reqID)
          terminate(progress)
        } // Create a response message and send it on the bus "back to the GUI"
      }


    }





      //--------FUNCTIONS: ----------------------------------------------------------------------------------


      // Creates a SOP and zone SOP map, given a robot schedule and some operations and stuff.
      def addRobot(zoneMap: Map[String, List[List[Operation]]], zoneSopMapping :  Map[String, Seq[SOP]],
                   activeZones: Set[String], robotCommands : List[String],
                   availableOps: List[Operation],
                   robotSchedule: String, hierarchyRoot: HierarchyRoot, ops: List[Operation],
                   collector: CollectorModel, h: SPAttributes): (Map[String, List[List[Operation]]], Set[String], List[SOP], Map[String, Seq[SOP]]) = {


        var zMap = zoneMap
        var activeZoneSet = activeZones
        var availableOperations = availableOps
        var zoneSopMap = zoneSopMapping

        var ss = List[SOP]() // The SOP that will eventually be sent back to the GUI for display.

        robotCommands.foreach(command => { // Evaluate each robot command

          // Active Zones
          if (command.startsWith("WaitSignal AllocateZone")) {
            val zoneIndex = command.indexOf("Zone")  // Gets the index of where the String Zone starts of the robot command
            val zoneStr = cleanName(command.substring(zoneIndex), true) // removes other parts of the command not containing the name of the zone
            activeZoneSet += zoneStr // Adds the zone to a set of active Zones, being a set it can only contain one instance of the zone.
          }

          else if (command.startsWith("WaitSignal ReleaseZone")) {

            val zoneIndex = command.indexOf("Zone") // Get the index of the word zone in the command string
            val zoneStr = cleanName(command.substring(zoneIndex), true) // remove things around the string
            activeZoneSet -= zoneStr // the zone is no longer active, remove it from the set

            if(zMap.contains(zoneStr)) {
              val zoneSequences = zMap(zoneStr).map(opList =>  Sequence(opList.map(op => Hierarchy(op.id)):_*) ).toSeq // Goes through the operation lists, and for each operation creates a Hierarchy. The Hierarchies are placed in list in Sequences which are also in a list, but transformed to a Seq.
              zMap -= zoneStr // removes the entry of this zone in the zone map
              if(zoneSopMap.contains(zoneStr)) zoneSopMap += zoneStr -> (zoneSopMap(zoneStr) ++ zoneSequences) else  zoneSopMap += zoneStr ->  zoneSequences // add the Zone sequence SOP to the zone sop map
            }
            }


          else if (command.startsWith("WaitCase")) { // Check if any of the "Commands" contains a string with a Case statement.

            val CaseSeparatedInAlphaNumericalList = command.split("\\W+")  // Take the case statement and split it into parts of alphanumerical symbols
            var allCaseSopSeqs = List[List[SOP]]()
            var zMapTmpList = List[Map[String,List[List[Operation]]]]() // Creates a new List for the Zon maps where all the operations from the Cases will be saved
            var activeZoneSetNew = Set[String]() // The active zones after the Cases will have changed, so here is a new Set
            CaseSeparatedInAlphaNumericalList.foreach(caseString => // Go through each case part and check if the part exists as an operation in the availableOps tree.

              availableOperations.find(caseOp => caseOp.name == caseString) // find an operation with the same name as the robot command's case
              match {
                case Some(caseOp) => // If there is an operation that matches the case
                  val operationChildIds = hierarchyRoot.getChildren(caseOp.id) // Get the kids ids of the Case operation
                var robotCommandsInChild = List[String]() // Init a new robot command list, to use in recursion of this function
                  var newOp = caseOp // Init a new Operation, could be anything just needs the correct type here.
                var zMaptmp = Map[String, List[List[Operation]]]() // This is the place zones and operations will be mapped for each new case.
                  operationChildIds.foreach(opChildId => ops.find(opChild => opChild.id == opChildId) // Find the Child operation of the case using its ID, ops contains all of the operation in the whole Model
                  match {
                    case Some(opChild) =>

                      if (opChild.name == caseString) { // The first operation within the case has the same name as the actual Case, and may contain a robot schedule, at least for the PS model in this project
                        robotCommandsInChild = opChild.attributes.getAs[List[String]]("robotcommands").getOrElse(List()) // Get the robot commands as a list of strings, if there are none, then empty list.

                        val newId = ID.newID // Create a new ID for the operation, so that it can be saved in a new hierarchy separate from the old one.
                        newOp = opChild.copy(name = robotSchedule + "_" + opChild.name, attributes = opChild.attributes merge // Copy the operation with new, name, ID and attributes
                          SPAttributes("robotSchedule" -> robotSchedule, "original" -> opChild.id, "newID" -> newId),id = newId)

                        activeZoneSet.foreach(z => {zMaptmp += z ->  List(List(newOp)) } ) // Add the operation to the Zone map, checking which zones are active
                        collector.opWithID(newOp.name, Seq(newOp.attributes merge h), newId) // Save the operation in the collector

                        if(robotCommandsInChild.isEmpty) {
                          zMapTmpList :+= zMaptmp
                          activeZoneSetNew = activeZoneSetNew.union(activeZoneSet) // If there is a case with no operations following, then all the zones that were active before can be active afterwards too.
                        } // adds the zone map with only one operation to the list of maps in case there are no robot commands, since then there will be no further operations executing in this case.
                      }
                      availableOperations :+= opChild // Add the child operation of the case to the list of available operations.
                    case None => // do nothing
                  }
                  )

                  var caseSopSeqs = List[SOP]()
                  if (robotCommandsInChild.nonEmpty) { // If there are robotcommands this might mean that there are more operations that can be executed and added. If so recursion will be utilized to get SOP and Zone information about them.
                    val (zMapCaseTmp, activeZonesTmp, sopSeqsTmp, zSopMapTmp) = addRobot(zMaptmp, zoneSopMap, activeZoneSet, robotCommandsInChild, availableOperations, robotSchedule, hierarchyRoot, ops, collector, h) // call the function again
                    zMapTmpList :+= zMapCaseTmp // Adds the zonemap of the Case to the Zonemap list
                    activeZoneSetNew = activeZoneSetNew.union(activeZonesTmp) // Adds the active zones of the Case
                    caseSopSeqs = sopSeqsTmp // Gets the SOP from the Case
                    zoneSopMap = zSopMapTmp // Gets the zone SOP from the Case
                  }
                  caseSopSeqs = if (caseSopSeqs.nonEmpty) List(Sequence(SOP(Hierarchy((newOp.id)))) ++ caseSopSeqs(0).sop) else List(Sequence(SOP(Hierarchy((newOp.id))))) // add the initial Case operation to the start of the SOP
                  allCaseSopSeqs :+= caseSopSeqs // Add the Case SOP to the list of case SOPs
                case none => // do nothing
              })
            activeZoneSet = activeZoneSetNew // Updates the active zones with the active zones of the cases.

            var zMapNew = Map[String,List[List[Operation]]]() //Creating a new ZoneMap from the Cases zonemaps
            zMapTmpList.foreach(zM => zM.keys.foreach(z => { // Go through each of the zonemaps, get the keys for a zonemap, for every key
              var newOpLists = List[List[Operation]]() // Create a new list to add lists of operations to
              if (zMap.contains(z)) // Check if the original zone map contains a key of one of the new zone maps
                newOpLists= zMap(z).map(opList => { zM(z).map(zMOplist => opList ++ zMOplist)}).flatten // if it does, then I want to append all of the new Op lists to the end of every old one.
              else newOpLists= zM(z) // Otherwise it is only necessary to add the new list of op lists

              if(zMapNew.contains(z)) zMapNew += z -> (zMapNew(z) ++ newOpLists ) else zMapNew += z -> newOpLists // Then add this to the new zone map and repeat.
            }))
            zMap = zMapNew // Update the zone map

            val caseSeqOfSops = allCaseSopSeqs.filterNot(sopList => sopList.isEmpty).map(sopList => Sequence(SOP()) <-- sopList(0).sop).toSeq // removes empty sop lists, then maps each sop to a sequence instead

            if (caseSeqOfSops.nonEmpty) {
              var AlternativeSops = Alternative(SOP()) <-- caseSeqOfSops // Creates alternatives out of the seq[Sop]
              ss = List(if (!ss.isEmpty) Sequence(SOP()) <-- (ss(0).sop :+ AlternativeSops) else Sequence(AlternativeSops)) // adds the alternative Sop to the SOP that will be returned by the function
            }
          }
          else if (command.startsWith("!")) {
          } // do nothing, because that means the line of the robot schedule is commented
          else {
            val cleanOpName = cleanName(command, false)
            availableOps.find(operation => operation.name == cleanOpName) match {
              // find the current command in the list of available operations.
              case Some(operation) =>
                val opId = ID.newID // Create a new ID for the operation
                val newOp = operation.copy(name = robotSchedule + "_" + operation.name, attributes = operation.attributes merge
                  SPAttributes("robotSchedule" -> robotSchedule, "original" -> operation.id, "newID" -> opId),id = opId) // Create a new operation out of the old one, with new ID, name and attributes

                activeZoneSet.foreach(z => {if(zMap.contains(z)) zMap += z -> (zMap(z).map(opList => opList :+ newOp) ) else zMap += z ->  List(List(newOp)) } ) // adds the operation to the end of all existing operation lists mapped to the active zones
                collector.opWithID(newOp.name, Seq(newOp.attributes merge h), opId) // adds the operation to the collector

                ss = List(if (!ss.isEmpty) Sequence(SOP()) <-- (ss(0).sop :+ SOP(Hierarchy(opId))) else Sequence(SOP(Hierarchy(opId)) )) // Adds the operation to the SOP that will later be sent back from the function
              case none =>
            }
          }
        })
        (zMap, activeZoneSet, ss, zoneSopMap) // return zone map, active zones, SOP, and zone SOP map.
      }


      // Creates a SOP for a device resource. Right now only for a certain type of turnatable fixtures.
      def addOtherResource(rOps: List[Operation], rs: String, h: SPAttributes, collector: CollectorModel): SOP ={

        var activeOps = List[Operation]() // this will contain a list of the operations that are actually supposed to be executed, in that order, It is used to create a SOP
        // Todo : The functions used for the formal synthesis do not handle "-" symbols. So either change those functions or change the operation names here. It would be best to change the functions.
        // This could be defined somewhere else or sent from the GUI. It could be modified to create parallel sequences by adding a list which the nextstates Array can reside in and then loop over that list adding the Sequences as alternatives if desired.
        var currentState = "HOME" // The starting state, which will then be updated with current state, i.e next state in nextStates
        var nextStates = Array("OpenSeq2", "CloseSeq1","CloseSeq2","CloseSeq3","CloseSeq4","CloseSeq5","OpenSeq1","OpenSeq2")

        nextStates.foreach(nextState =>
        {
          var foundOp = rOps.find(op=> op.attributes.getAs[String]("source_pose").getOrElse("") == currentState && op.attributes.getAs[String]("target_pose").getOrElse("")   == nextState) // In each device operation there exists a source pose and a target pose, check which operation poses corresponds to the current and next State.
          if(foundOp.nonEmpty) {
            val newId = ID.newID // Create a new ID to make a copy of the operation
            val newOp = foundOp.get.copy(name = rs + "_" + foundOp.get.name.replace('-', '_'), attributes = foundOp.get.attributes merge  // There seems to be a problem with having "-" chars in the synthesis, have not found where... Which is why I change it here
              SPAttributes("robotSchedule" -> rs, "original" -> foundOp.get.id, "newID" -> newId),id = newId)
            collector.opWithID(newOp.name, Seq(newOp.attributes merge h), newId) // Add the operation to the Collector
            activeOps :+= newOp
            currentState = nextState}})

        (Sequence(SOP()) <-- activeOps.map(op=> Hierarchy(op.id)).toSeq)
      }


      def opsAtLevel(node: HierarchyNode, ops: List[Operation]): List[Operation] = {
        ops.filter(o=>node.children.exists(c=>c.item == o.id))
      }

      def cleanName(str: String, rmvSlash : Boolean): String = {

        val s = if(!str.startsWith("''  -  '")) str else {
          val ns = str.substring(8,str.length)
          val p = ns.indexOf("'")
          if(p == -1) ns else ns.substring(0,p)
        }

        val pos = s.indexOf(";")
        val cleanStr = if(pos < 0) s else s.substring(0,pos)

        if (rmvSlash)
          cleanStr.split("""\\""")(0) // remove everything with and after backslash \
        else
          cleanStr
      }

      def findParent(id: ID, node: HierarchyNode): Option[HierarchyNode] = {
        if(node.children.exists(_.item == id)) Some(node)
        else {
          val res = node.children.map(findParent(id,_)).flatMap(x=>x)
          if(res.isEmpty) None
          else Some(res.head)
        }
      }

      def robotScheduleVariable(rs: String) = "v"+rs+"_pos"

      def updateOpSequenceFromCollector(operations : List[Operation], collector : CollectorModel) : List[Operation]={ // Update the operation sequence with operations from the collector, which also contains transitions

        import CollectorModelImplicits._
        val uids = collector.parseToIDablesWithIDs()
        var collectorOps = uids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
        operations.flatMap(op =>  collectorOps.filter(cOps => cOps.attributes.getAs[ID]("original").get == op.attributes.getAs[ID]("original").get))
      }

      def getOpsFromCollector(collector : CollectorModel) : List[Operation]={ // Get the operations from the collector
        import CollectorModelImplicits._
        val uids = collector.parseToIDablesWithIDs()
        uids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
      }

      def extractFromSOP(individualSop :SOP, startCond : String, reallyLast : Boolean, collector : CollectorModel, ops : List[Operation], h : SPAttributes, checkedTime : Boolean) : (List[List[Operation]], String, SOP) ={
        var startCondition = startCond
        var newOpSequences = List[List[Operation]]()
        var lastNode = false
        var newSop = SOP()


        if(individualSop.isInstanceOf[Sequence]) {

          individualSop.sop.foreach(sopNode => {
            var tmpOpSequences = List[List[Operation]]()

            if(sopNode == individualSop.sop.last && reallyLast == true){ // Check if this is the last node of the sop being processed.
              lastNode = true
            }
            var (opSeqs, newStartCond, seqSop) = extractFromSOP(sopNode, startCondition , lastNode, collector,ops, h , checkedTime) // call the function again with the subNode Sop
            startCondition = newStartCond
            if(newSop.isEmpty) newSop = Sequence(SOP()) <-- Seq(seqSop) else newSop += seqSop

            opSeqs.foreach(opSeq =>{
              if(newOpSequences.isEmpty) {tmpOpSequences :+= opSeq}
              else {newOpSequences.foreach(opsInSeq => tmpOpSequences :+= opsInSeq ++ opSeq)}
            }) // for each opSeq, append all of its sequences to the already existing ones. Create new lists as required.
            newOpSequences = tmpOpSequences
          })
        }


        else if(individualSop.isInstanceOf[Hierarchy]){
          var op = ops.filter(op => op.id == individualSop.asInstanceOf[Hierarchy].operation).head // get the ID from the node, find the corresponding operation

          var newId = ID.newID // Create a new ID for the op.
          val robotSchedule = op.attributes.getAs[String]("robotSchedule").getOrElse("error")
          op = op.copy(name = op.name, attributes = op.attributes merge
            SPAttributes("robotSchedule" -> robotSchedule, "original" -> op.id, "newID" -> newId),id = newId)

          startCondition = addOpToCollector (op, startCondition,reallyLast, collector,h,checkedTime) // add the operation to the collector and get the new startCondition as that ops endCond

          newOpSequences :+= List(op) // add the operation to the list
          newSop = SOP() <-- Seq(Hierarchy(newId))
        }

        else if(individualSop.isInstanceOf[Alternative]){
          var opSeqstmp = List[List[Operation]]()
          var newStartCondList = List[String]() // Save the end conditions from the alternatives here in a list
          var altSops = Seq[SOP]()
          individualSop.sop.foreach(subNode => {
            var (altSeqs, altStartCond, altSop) = extractFromSOP(subNode, startCondition , reallyLast, collector,ops, h , checkedTime) // call the function again with the subNode Sop
            newStartCondList :+= altStartCond
            altSops :+= altSop
            altSeqs.foreach(altSeq => {
              if(newOpSequences.isEmpty){opSeqstmp :+= altSeq }
              else // for each alternative, append all of its sequences to the already existing ones. Create new lists as required.
                newOpSequences.foreach(opsInSeq => opSeqstmp :+= opsInSeq ++ altSeq )
            })
          })
          startCondition = newStartCondList.mkString("", " OR ", "") // Create a new StartingConditon from the List i.e : "op1_end OR op2_end .etc...)
          newOpSequences = opSeqstmp // update the opSeq list

          if(newSop.isEmpty) newSop = SOP() <-- Seq((Alternative(SOP()) <-- altSops)) else  newSop += (Alternative(SOP()) <-- altSops)
        }

        (newOpSequences, startCondition, newSop) // return this
      }

      def addOpToCollector (op : Operation, startcond : String, lastNode : Boolean, collector : CollectorModel, h : SPAttributes, checkedTime : Boolean) : String={
        val rs = op.attributes.getAs[String]("robotSchedule").getOrElse("error")
        val endCondition = if(!lastNode) op.name + "_done" else "idle"  // If its the last Node then the sop should be able to start over from "idle"..
        val trans = SPAttributes(collector.aResourceTrans(rs, startcond, op.name, endCondition))
        var duration =  op.attributes.getAs[Double]("duration").getOrElse(0.0)
        val spAttrDuration = if(duration > 0.0 || !checkedTime) SPAttributes("duration" -> duration) else SPAttributes("duration" -> 1.0)
        collector.opWithID(op.name, Seq(op.attributes merge trans merge h merge spAttrDuration), op.id)
        endCondition
      } // add operations to the collector, with transition conditions.


    //-------------------------------------------------------------------------------------------------------------------------
    case _ => sender ! SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }

}



