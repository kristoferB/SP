// Import stuff and setup the RobotScheduleSetup class
package sp.virtcom

import java.util.UUID

import akka.actor._
import sp.system._
import sp.system.messages.{KeyDefinition, _}
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

 // println("Starting RobotOptimization !!!!!!    ------------------------------------------------------------>")
  val timeFactor = 1000.0
  def test = {
    val duration = ops.map(o=>(o.attributes.getAs[Double]("duration").getOrElse(0.0) * timeFactor).round.toInt).toArray
    val indexMap = ops.map(_.id).zipWithIndex.toMap
    val numOps = ops.size
    val totalDuration = duration.sum

 //   println("numOps:    " + numOps)

    // start times, end times, makespan
    var s = Array.fill(numOps)(CPIntVar(0, totalDuration))
    var e = Array.fill(numOps)(CPIntVar(0, totalDuration))
    var m = CPIntVar(0 to totalDuration)

    var extra = Array.fill(mutexes.size)(CPBoolVar())

    forceEndTimes.foreach { case (t1,t2) => add(e(indexMap(t1)) == s(indexMap(t2))) }
  //  println("precedences     !")

    precedences.foreach { case (t1,t2) => add(e(indexMap(t1)) <= s(indexMap(t2))) }
  //  println("mutexes     !")
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
  //  println("ops.foreach     !")
    ops.foreach { op =>
      // except for time 0, operations can only start when something finishes
      // must exist a better way to write this
      add(e(indexMap(op.id)) == s(indexMap(op.id)) + duration(indexMap(op.id)))
      val c = CPIntVar(0, numOps)
      add(countEq(c, e, s(indexMap(op.id))))
      // NOTE: only works when all tasks have a duration>0
      add(s(indexMap(op.id)) === 0 || (c >>= 0))    
    }
   // println(" add(maximum(e, m))    !")
    add(maximum(e, m))

   // println("minimize(m)      !")
    minimize(m)
  //  println("binaryFirstFail:    " )
    search(binaryFirstFail(extra++s++Array(m)))

    var sols = Map[Int, Int]()
    var ss = Map[Int,List[(ID,Int,Int)]]()
    onSolution {
      sols += m.value -> (sols.get(m.value).getOrElse(0) + 1)
   //   println("Makespan: " + m.value)
    //  println("Start times: ")
      ops.foreach { op =>
     //   println(op.name + ": " + s(indexMap(op.id)).value + " - " +
         // duration(indexMap(op.id)) + " --> " + e(indexMap(op.id)).value)
      }
      sols.foreach { case (k,v) => println(k + ": " + v + " solutions") }
      val ns = ops.map { op => (op.id, s(indexMap(op.id)).value,e(indexMap(op.id)).value) }
      ss += m.value->ns
    }
  //  println("After onSolution------------------------------------------------------------------------------------------------")

    val stats = start(timeLimit = 60) // (nSols =1, timeLimit = 60)
  //  println("===== oscar stats =====\n" + stats)

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
 // println("done Test")
}

// Specifies what type of object VolvoRobotSchedule will receive
object VolvoRobotSchedule extends SPService {
  val specification = SPAttributes(
    "command" -> KeyDefinition("String", List(), None),
    "SopID"   -> KeyDefinition("String", List(), None),
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
    TransformValue("SopID", _.getAs[String]("SopID"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(sh: ActorRef) = ServiceLauncher.props(Props(classOf[VolvoRobotSchedule], sh))
}


class VolvoRobotSchedule(sh: ActorRef) extends Actor with ServiceSupport with AddHierarchies with MakeASop{
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher

  // Receive message from gui with selected schedules, their properties and ids as well as the reqID of this instance.
  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      // Init request and reply and progress update
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler) // these progress messages are required to be initialized and sent even tho they are not used much here.
      progress ! SPAttributes("progress" -> "starting volvo robot schedule")
      // Get the setup and command information from the message
      val setup = transform(VolvoRobotSchedule.transformTuple._1)
      val command = transform(VolvoRobotSchedule.transformTuple._2)

      // filter out the operations from the ids
      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
      val schedules = ops.filter(op => setup.selectedSchedules.contains(op.id))

      // todo: find the correct hierarchy root
      val hierarchyRoots = ids.filter(_.isInstanceOf[HierarchyRoot]).map(_.asInstanceOf[HierarchyRoot])

      var robotOpIdMapN = Map[Operation, ID]()

       // hierarchyRoots.foreach(hRoot => {robotOpIdMapN ++=  schedules.map(s=> {   s->hRoot.children.map(findParent(s.id,_)).flatMap(x=>x).head.item }).toMap  })

      hierarchyRoots.foreach(hRoot => { schedules.foreach(s=>
         {  if(! hRoot.children.map(findParent(s.id,_)).flatMap(x=>x).isEmpty)
            {  robotOpIdMapN += s->hRoot.children.map(findParent(s.id,_)).flatMap(x=>x).head.item}})})




     // val robotOpIdMapN = schedules.map(s=>s->hierarchyRoots.foreach(hRoot=> hRoot.children.map(findParent(s.id,_)).flatMap(x=>x).head.item).toMap


     // val robotOpIdMap = schedules.map(s=>s->hierarchyRoot.children.map(findParent(s.id,_)).flatMap(x=>x).head.item).toMap
      val robotOpMap = robotOpIdMapN.flatMap{case (k,v)=>
        val x = ids.find(_.id==v)
        x match {
          case None => None
          case Some(idable) => Some(k->idable)
        }}.toMap
      val scheduleNames = schedules.map(op => {if(! op.attributes.getAs[List[String]]("robotcommands").getOrElse(List()).isEmpty) op.name else robotOpMap(op).name}).toSet.mkString("_")

      val h = SPAttributes("hierarchy" -> Set("VRS_"+ scheduleNames))
      case class VolvoRobotScheduleCollector(val modelName: String = "VolvoRobotSchedule") extends CollectorModel
      val collector = VolvoRobotScheduleCollector()
      val idle = "idle"
      var uids = List[IDAble with Product with Serializable]()



      def updateOpSequenceFromCollector(operations : List[Operation]) : List[Operation]={

        import CollectorModelImplicits._
        uids = collector.parseToIDablesWithIDs()
        var allOperationsWithTransitions = uids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])

        var newOplist = List[Operation]()
        operations.foreach(operation =>{
          var opOrigID = operation.attributes.getAs[ID]("original").get
          allOperationsWithTransitions.foreach(opWithTrans => if(opWithTrans.attributes.getAs[ID]("original").get == opOrigID) newOplist :+= opWithTrans)
        })
        newOplist
      }


/*


      if(command == "GenerateSops"){

        val zoneMapsAndOpsNew = schedules.zipWithIndex.map { case (op,i) =>

          val hierarchyRoot = hierarchyRoots.head
          // find the right level among the hierarchy nodes
          val p = hierarchyRoot.children.map(findParent(op.id,_)).flatMap(x=>x).head
          val pops = opsAtLevel(p,ops) // Gets the topmost operation names of the selected hirarchy

          val robcmds = op.attributes.getAs[List[String]]("robotcommands").getOrElse(List()) // Gets all of the robotcommands for the selected object/schedule
          val rs = robotOpMap(op).name      // The name of the Selected object

          collector.v(robotScheduleVariable(rs), idleValue = Some(idle), attributes = h)
            splitIntoOpsAndZonesNew(LinkedHashMap[Operation, Set[String]](), Map[String, Seq[SOP]](), List(), Set(), robcmds, pops, "", rs, hierarchyRoot, ops, collector, h, "")
        }

        import CollectorModelImplicits._
        uids = collector.parseToIDablesWithIDs()  // Extracts the uids from the collector, this can contain operations and transition things

        val sops = zoneMapsAndOpsNew.map { x => x._4 }.flatten
        val sopspec = SOPSpec(schedules.map(_.name).toSet.mkString("_"), sops, h)

        var zonespecs = Iterable[SOPSpec]()
        zoneMapsAndOpsNew.map { x =>
          var zoneSopMap = x._6
          zonespecs = zoneSopMap.map { case (z, l) => SOPSpec(z, List((Arbitrary(SOP()) <-- l)), h) }
        }

        val nids = List(sopspec) ++ zonespecs ++ uids
        val hids = nids ++ addHierarchies(nids, "hierarchy")

        // Create a response message and send it on the bus "back to the GUI"
        val resAttr = SPAttributes()
        replyTo ! Response(hids,resAttr, rnr.req.service, rnr.req.reqID)
        terminate(progress)
      } */





      if(command == "GenerateSops"){
        var zoneMap = LinkedHashMap[Operation, Set[String]]()
        var opList = List[Operation]()
        var activeZones = Set[String]()
        var plcStartCond = ""
        var zoneSopMapping = Map[String, Seq[SOP]]()
        val zoneMapsAndOpsNew = schedules.zipWithIndex.map { case (op,i) =>

          val hierarchyRoot = hierarchyRoots.head
          // find the right level among the hierarchy nodes
          val p = hierarchyRoot.children.map(findParent(op.id,_)).flatMap(x=>x).head
          val pops = opsAtLevel(p,ops) // Gets the topmost operation names of the selected hirarchy

          val robcmds = op.attributes.getAs[List[String]]("robotcommands").getOrElse(List()) // Gets all of the robotcommands for the selected object/schedule
        val rs = robotOpMap(op).name      // The name of the Selected object
          collector.v(robotScheduleVariable(rs), idleValue = Some(idle), attributes = h)
          //TODO How does this works? Will it send in one robot schedule at the time or all at ones?
          var result = splitIntoOpsAndZonesNew(zoneMap, zoneSopMapping, opList, activeZones, robcmds, pops, plcStartCond, rs, hierarchyRoot, ops, collector, h, "")
          zoneMap = result._1
          zoneSopMapping = result._6
          opList = result._2
          activeZones = result._3
          plcStartCond = result._5
          result
          // zoneMap = result._1
          //println("--------------------Test4--------------------------")
        }

        import CollectorModelImplicits._
        uids = collector.parseToIDablesWithIDs()  // Extracts the uids from the collector, this can contain operations and transition things

        val sops = zoneMapsAndOpsNew.map { x => x._4 }.flatten
        val sopspec = SOPSpec(schedules.map(_.name).toSet.mkString("_"), sops, h)

        var zonespecs = Iterable[SOPSpec]()
        zoneMapsAndOpsNew.map { x =>
          var zoneSopMap = x._6
          zonespecs = zoneSopMap.map { case (z, l) => SOPSpec(z, List((Arbitrary(SOP()) <-- l)), h) }
        }

        val nids = List(sopspec) ++ zonespecs ++ uids
        val hids = nids ++ addHierarchies(nids, "hierarchy")

        // Create a response message and send it on the bus "back to the GUI"
        val resAttr = SPAttributes()
        replyTo ! Response(hids,resAttr, rnr.req.service, rnr.req.reqID)
        terminate(progress)
      }





      else if(command == "calculateUsingSops") {


        val SopID = transform(VolvoRobotSchedule.transformTuple._3) // Get the SOP ID from the message, sent from the GUI
        // Get the Parent of the SOP (top node). Then get the kids of that node. Find all SopSpecs and filter out the Main SopSpec from the Zone/Station -SopSpecs


        // Get the parent of the selected sop... This could be HierarchyNode or a HierarchyRoot, fix this please..
        var parentNodeToSoplist = List[HierarchyNode]()
        var parentroot = List[HierarchyRoot]()
        hierarchyRoots.foreach(hRoot => {
          hRoot.children.foreach(child => {if(child.item.toString == SopID){ parentroot = List(hRoot)}}) // hierarchyroot or node...
        })
        var parentNodeToSop =parentroot.head


        val allNodesInHierarchy = ids.filter(o=>parentNodeToSop.children.exists(c=>c.item == o.id)) // Get all of the Hierarchy children as idable objects from ids
        val allSopSpecs = allNodesInHierarchy.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec])  // filter out the Sop Specs
        var sopSpec = allSopSpecs.find(c=> c.id.toString == SopID).get //Get the SOPSpec with the Right ID. ( the one in the GUI textbox)
        var zonespecs = if(allSopSpecs.size >1) allSopSpecs.diff(List(sopSpec)).toIterable  else Iterable[SOPSpec]()// Remove the main sopspec from the list, remaining are the zone/station sopspecs

        var ss = List(sopSpec.sop(0)) // The old prime SOP

        // Go through the list of schedules (operations) get their names and create Collector variables.
        //var rs =""
        schedules.zipWithIndex.map { case (op, i) =>
          val rs = robotOpMap(op).name
          collector.v(robotScheduleVariable(rs), idleValue = Some(idle), attributes = h) // Creates the transition variables for the Things! rs is the name of the robotschedule.
        }

        // Go through the SOPs for each resource, one by one and create straight operation sequences for the optimization.
        // Create new IDs for the operations, and new SOPs for those IDs
        var opSequences = List[List[Operation]]()
        var sopSeqs = Seq[SOP]()

        sopSpec.sop.foreach(individualSop => {
         var exOps =  extractOpsFromSOP(individualSop :SOP, "idle", true)
          var opSeqs = exOps._1
          sopSeqs :+= exOps._3
          opSequences ++= opSeqs // A list with lists for all possible sequences
        })

        ss = sopSeqs.toList
        sopSpec = SOPSpec(schedules.map(_.name).toSet.mkString("_"), ss,h)

        def extractOpsFromSOP(individualSop :SOP, startCond : String, reallyLast : Boolean) : (List[List[Operation]], String, SOP) ={
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
              var (opSeqs, newStartCond, seqSop) = extractOpsFromSOP(sopNode, startCondition , lastNode) // call the function again with the subNode Sop
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

            startCondition = addOpToCollector (op, startCondition,reallyLast) // add the operation to the collector and get the new startCondition as that ops endCond

            newOpSequences :+= List(op) // add the operation to the list
            newSop = SOP() <-- Seq(Hierarchy(newId))
          }

          else if(individualSop.isInstanceOf[Alternative]){
            var opSeqstmp = List[List[Operation]]()
            var newStartCondList = List[String]() // Save the end conditions from the alternatives here in a list
            var altSops = Seq[SOP]()
            individualSop.sop.foreach(subNode => {
              var (altSeqs, altStartCond, altSop) = extractOpsFromSOP(subNode, startCondition , reallyLast) // call the function again with the subNode Sop
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


        def addOpToCollector (op : Operation, startcond : String, lastNode : Boolean) : String={
          val rs = op.attributes.getAs[String]("robotSchedule").getOrElse("error")
          val endCondition = if(!lastNode) op.name + "_done" else "idle"  // If its the last Node then the sop should be able to start over from "idle"..
          val trans = SPAttributes(collector.aResourceTrans(robotScheduleVariable(rs), startcond, op.name, endCondition))
          collector.opWithID(op.name, Seq(op.attributes merge trans merge h), op.attributes.getAs[ID]("newID").get)
          endCondition
        } // add operations to the collector, with transition conditions.

        import CollectorModelImplicits._
        uids = collector.parseToIDablesWithIDs() // extract variables, transitions and operations from collector

        var operations = uids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation]) // Get all operations from the collector

        // Create zoneMap
        var zoneMapping = Map[Operation,Set[String]]()
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

      // For the synthesis:
      val nids = List(sopSpec) ++ zonespecs ++ uids
      val hids = nids ++ addHierarchies(nids, "hierarchy")



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



/* Tried putting this outside of the foreach oplist...
        if (operations.size > 1) {
          val np = operations zip operations.tail
          if(!zoneMapping.isEmpty) { // Checks which operations have the same zoneMap, and kinda lists them... in some obscure manner
          val fe = np.filter { case (o1, o2) => zoneMapping(o1) == zoneMapping(o2) }.map { case (o1, o2) => (o1.id, o2.id) }
            forceEndTimes ++= fe
          }
        }*/

        var allops = operations // save values


      //------------------------------------------------------------------------------------------------------------------

        // going through each oplist and adding information to the precedence and forceEndTime lists
        opSequences.foreach(oplist => {

          //operations = oplist
          operations = updateOpSequenceFromCollector(oplist)


          // Creates a new Zonemap where all of the zonemap operations are contained in the current oplist
          var zoneMappingNew = Map[Operation,Set[String]]()
          operations.foreach(op => zoneMappingNew += (op -> Set()))
          zoneMappingList.foreach(zMap => { if(zMap.keySet subsetOf operations.toSet) { zMap.keys.foreach(op => zoneMappingNew += (op -> (zoneMappingNew(op) ++ zMap(op))))}})


        //  println("     zoneMappingList                "    + zoneMappingList)

        //  println("     zoneMappingNew                "    + zoneMappingNew)
          /* Adds transition conditions to the operations....  But since they are not used anywhere, it is unnecessary.
          operations.foldLeft(idle) { case (s, o) => {
          val done = if (operations.reverse.head == o) idle else o.name + "_done" // go back to init
          val rs = o.attributes.getAs[String]("robotSchedule").getOrElse("error")
          val trans = SPAttributes(collector.aResourceTrans(robotScheduleVariable(rs), s, o.name, done))
          collector.opWithID(o.name, Seq(o.attributes merge trans merge h), o.attributes.getAs[ID]("newID").get)
          done
        }
        }*/

          var zoneMap = zoneMappingNew//zoneMapping
         // var precedences: List[(ID,ID)] = List()
         // var mutexes: List[(ID,ID)] = List()
          // var forceEndTimes: List[(ID,ID)] = List()
        // operations = updateOpSequenceFromCollector(operations)


          // ToDo: Change this...
        // Creates a new ZoneMap for the current operation Sequence, containing only those operations.

          // makes sure that the zonemap has the right IDs
          val zoness = zoneMap.map { case (o, zoness) => zoness }.flatten.toSet
        var newZoneMap = Map[Operation, Set[String]]()
        zoneMap.map { case (o, zoness) => {
          var opNewID = o.attributes.getAs[ID]("newID").get
          operations.foreach({ opWithTrans => if (opWithTrans.attributes.getAs[ID]("newID").get == opNewID) newZoneMap += (opWithTrans -> zoness) })
        }
        }
        zoneMap = newZoneMap // This is scrambled, does it matter? It does not seem that way.

        //  println("     zoneMap                "    + zoneMap)


          // For the CP solver-----------
          if (operations.size > 1) {
            val np = operations zip operations.tail
            precedences ++= np.map { case (o1, o2) => (o1.id, o2.id) } // Orders all operations after eachother in the order they were given from the op list
            if(!zoneMap.isEmpty) { // Checks which operations have the same zoneMap, and kinda lists them... in some obscure manner
              val fe = np.filter { case (o1, o2) => zoneMap(o1) == zoneMap(o2) }.map { case (o1, o2) => (o1.id, o2.id) } // Seems to do it like: If there are more than one operation with the same zones, then they are placed in fe.
                                                                                                                        // Like: op1: in zones: Station1,Zone1
                                                                                                                        //        op2: in zones: Station1,Zone1 --> fe: (op1,op2),
              forceEndTimes ++= fe
            }
          }

          operations.foreach { op =>
            println("Operation " + op.name + " ID: " + op.id + " in zones: " + zoneMap(op).mkString(","))
          }

        // create forbidden zones
/*
        val zones = zoneMap.map { case (o, zones) => zones }.flatten.toSet
          /*
                    zones.map { zone =>
                      var opsInZone = zoneMap.filter { case (o, zones) => zones.contains(zone) }.map(_._1)

                      val sortedOpsInZone = operations.flatMap(x => opsInZone.find(opInZone => {
                        opInZone == x
                      }))
                      opsInZone = sortedOpsInZone //.map(op=> op.id)

                      val opids = sortedOpsInZone.map(op => op.id)
                      mutexes ++= opids.zip(opids.tail)
                    } */


          val zoneSeqs = zones.map { zone =>
            val opsInZone = zoneMap.filter { case (o,zones) => zones.contains(zone) }.map(_._1)

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
        
*/
        // Run the CP optimization
/*
          println("mutexes        :::::    "  + mutexes)
          println("forceEndTimes :::::   " + forceEndTimes  )

          println("precedences :::::   " + precedences  )
*/
         // println("operations :::::   " + operations  )
           //operations = List()
         // precedences = List()
          // mutexes = List()
         //  forceEndTimes = List()
/*
        val ro = new RobotOptimization(allops, precedences, mutexes, forceEndTimes)
        val roFuture = Future {
          ro.test
        }


        for {
          (cpCompl, cpTime, cpSols) <- roFuture
          gantt = cpSols.map { case (makespan, sop, gantt) =>
            (gantt)
          }.flatten

          makespan = cpSols.map { case (makespan, sop, gantt) =>
            (makespan)
          }

        }yield{
          makespans ++= makespan
          println("makespan          "   + makespan)
          println("gantt      "    + gantt)
          gantt.foreach(timedOp =>   {
            var alreadyExists = 0
            ganttChart.foreach(elem => if(elem._1 == timedOp._1) alreadyExists = 1)
            if (alreadyExists == 0)
              ganttChart :+= timedOp
          })
        } */
      })
mutexes = mutexes.distinct
forceEndTimes = forceEndTimes.distinct
precedences = precedences.distinct
        // When all of the operation sequences have been processed and the precedences + forceEndTimes have been created.
        // The mutexes which are just given by the great zonemap and all operations + precedences & forceEndTimes are sent to the robot optimization.
        // The optimization is working for straight sequences, but when the
        println("mutexes        :::::    "  + mutexes)
        println("forceEndTimes :::::   " + forceEndTimes  )

        println("precedences :::::   " + precedences  )
		 // println("allops     .::::   "   + allops)
		  
        val ro = new RobotOptimization(allops, precedences, mutexes, forceEndTimes)
        val roFuture = Future {
          ro.test
        }

        for {
          (cpCompl, cpTime, cpSols) <- roFuture
          gantt = cpSols.map { case (makespan, sop, gantt) =>
            (gantt)
          }.flatten

          makespan = cpSols.map { case (makespan, sop, gantt) =>
            (makespan)
          }

        }yield{
          makespans = makespan
          ganttChart = gantt
          println("makespan          "   + makespan)
          println("gantt      "    + gantt)
        }






      //--------------------------------------------------------------------------------------------------------------------------
      //--------------------------------------------------------------------------------------------------------------------------
      //--------------------------------------------------------------------------------------------------------------------------
      // now, extend model and run synthesis
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

      /*
        //Get info about the CP solution
        (cpCompl, cpTime, cpSols) <- roFuture
        sops = cpSols.map { case (makespan, sop, gantt) =>
          (makespan, SOPSpec(s"path_${makespan}", sop), gantt)
        }.sortBy(_._1)*/

      } yield {
        // Create a response message and send it on the bus "back to the GUI"

       // println("makespans        "   + makespans)

      //  println("ganttChart        "   + ganttChart)
        val cpCompl = true
        val cpTime = 38
        val sops = (makespans.max, SOPSpec(s"path_${makespans.max}", ss), ganttChart)

        val resAttr = SPAttributes("numStates" -> numstates, "cpCompleted" -> cpCompl, "cpTime" -> cpTime, "cpSops" -> List(sops), "bddName" -> bddName)
        // println("resAttr          ______:::  " + resAttr)
        // println("List(sops).map(_._2)                 ::: "   + List(sops).map(_._2))
        // println("ids_merged2 :::::    "    + ids_merged2)
        replyTo ! Response(ids_merged2 ++ List(sops).map(_._2), resAttr, rnr.req.service, rnr.req.reqID)

        terminate(progress)
      }
      // })

      //  terminate(progress)
    }






    }





      //--------FUNCTIONS: ----------------------------------------------------------------------------------


      def splitIntoOpsAndZonesNew(zoneMap: LinkedHashMap[Operation, Set[String]], zoneSopMapping :  Map[String, Seq[SOP]],
                                  opList: List[Operation], activeZones: Set[String], cmds : List[String],
                                  availableOps: List[Operation], plcStartCond: String,
                                  robotSchedule: String, hierarchyRoot: HierarchyRoot, ops: List[Operation],
                                  collector: CollectorModel, h: SPAttributes, currentlyValidatingCase: String): (LinkedHashMap[Operation, Set[String]],List[Operation], Set[String], List[SOP], String, Map[String, Seq[SOP]], List[List[Operation]]) = {

        // Fix the ZoneMapping, the waitSignals PLC conditions, and the nonexisting operations
        var zoneMapping = zoneMap

        var operationList = opList
        var activeZoneList = activeZones
        var robotCommands = cmds
        var availableOperations = availableOps

        var plcSignalsStartConditions = plcStartCond
        var ss = List[SOP]()
        var lh = List[Hierarchy]()

        var zoneSopMap = zoneSopMapping
        var allOperationSequences = List[List[Operation]]()
        var currentCase = currentlyValidatingCase
        var caseListTmp = List[String]()
        var allocatedInCase = false

        robotCommands.foreach(command => {

          // Active Zones
          if (command.startsWith("WaitSignal AllocateZone")) {
            val zoneIndex = command.indexOf("Zone")
            val zoneStr = cleanName(command.substring(zoneIndex), true)
            activeZoneList += zoneStr+currentCase
          }

          else if (command.startsWith("WaitSignal ReleaseZone")) {
            val zoneIndex = command.indexOf("Zone")
            val zoneStr = cleanName(command.substring(zoneIndex),true)
            var containsCases = false
            val zones = zoneMapping.map { case (o, zones) => zones }.flatten.toSet

            var caseZones = Set[String]()
            // Enters is zones contains a zone with exactly the same name as zoneStr
            if (zones.contains(zoneStr)) {

              var zoneSOPSeq = Seq[SOP]()
              var opsInZone = zoneMapping.filter { case (o, zones) => zones.contains(zoneStr) }.map(_._1)

              opsInZone.foreach(opInZone => {
                zoneMapping(opInZone) -= zoneStr
              })

              zones.foreach(zone =>
                if(zone == zoneStr){

                }
                else if(zone.startsWith(zoneStr)){
                  containsCases = true
                  zoneSOPSeq = Seq[SOP]()
                  var opsInZoneCase = zoneMapping.filter{case (o, zones) => zones.contains(zone)}.map(_._1)

                  opsInZoneCase.foreach(opInZone => {
                    zoneMapping(opInZone) -= zoneStr
                  })
                  var opsInZoneWithCases = opsInZone ++ opsInZoneCase

                  opsInZoneWithCases.foreach(opInZone => zoneSOPSeq :+= SOP(Hierarchy((opInZone.attributes.getAs[ID]("newID").get))))
                  var sequenceOfOpsInZone = (Sequence(SOP()) <-- zoneSOPSeq)

                  var zoneSOP = SOP()

                  if (zoneSopMap.contains(zoneStr)) {
                    zoneSopMap += (zoneStr -> (zoneSopMap(zoneStr) :+ sequenceOfOpsInZone))
                  }
                  else {
                    zoneSopMap += (zoneStr -> List(sequenceOfOpsInZone))
                  }
                }
              )
              if (!containsCases){
                zoneSOPSeq = Seq[SOP]()

                opsInZone.foreach(opInZone => zoneSOPSeq :+= SOP(Hierarchy((opInZone.attributes.getAs[ID]("newID").get))))
                var sequenceOfOpsInZone = (Sequence(SOP()) <-- zoneSOPSeq)
                var zoneSOP = SOP()

                if (zoneSopMap.contains(zoneStr)) {
                  zoneSopMap += (zoneStr -> (zoneSopMap(zoneStr) :+ sequenceOfOpsInZone))
                }
                else {
                  zoneSopMap += (zoneStr -> List(sequenceOfOpsInZone))
                }
              }
            }
            else {
              zones.foreach(zone =>
                if(zone.startsWith(zoneStr)){
                  var zoneSOPSeq = Seq[SOP]()
                  // Filter out operations that contains the zoneStr
                  var opsInZone = zoneMapping.filter { case (o, zones) => zones.contains(zone) }.map(_._1)

                  // Something that might keeps track of cases. Destroys zoneMapping that are essential for the optimizing
                  opsInZone.foreach(opInZone => {
                    zoneMapping(opInZone)  -=   zoneStr
                  })
                  // Creates a SOP for all ops in zone
                  opsInZone.foreach(opInZone => zoneSOPSeq :+=  SOP(Hierarchy((opInZone.attributes.getAs[ID]("newID").get))))
                  var sequenceOfOpsInZone =  (Sequence(SOP()) <-- zoneSOPSeq)

                  // If there is a sop add the new one as a sequence, else add the new one as the only sop in a list
                  if (zoneSopMap.contains(zoneStr)) {
                    zoneSopMap += (zoneStr ->  (zoneSopMap(zoneStr) :+ sequenceOfOpsInZone) )
                  }
                  else {
                    zoneSopMap += (zoneStr ->  List(sequenceOfOpsInZone) )
                  }
                }
              )
            }

            /*val activeZoneListTmp = activeZoneList
            activeZoneListTmp.foreach(activeZone =>
              if(currentCase.length == 0) {
                if (activeZone.startsWith(zoneStr)) {
                  activeZoneList -= activeZone
                }
              }
              else{
                activeZoneList -= zoneStr+currentCase
              }
            )*/
            activeZoneList -= zoneStr+currentCase
            /*//    println("command.startsWith WaitSignal ReleaseZone" + command)
            val zoneIndex = command.indexOf("Zone")
            val zoneStr = cleanName(command.substring(zoneIndex), true)



            val zones = zoneMapping.map { case (o, zones) => zones }.flatten.toSet
            if (zones.contains(zoneStr)) {
              var zoneSOPSeq = Seq[SOP]()
              var opsInZone = zoneMapping.filter { case (o, zones) => zones.contains(zoneStr) }.map(_._1)


              opsInZone.foreach(opInZone => {
                zoneMapping(opInZone)  -=   zoneStr
              })

              opsInZone.foreach(opInZone => zoneSOPSeq :+=  SOP(Hierarchy((opInZone.attributes.getAs[ID]("newID").get))))
              var sequenceOfOpsInZone =  (Sequence(SOP()) <-- zoneSOPSeq)

              var zoneSOP = SOP()

              if (zoneSopMap.contains(zoneStr)) {
                zoneSopMap += (zoneStr ->  (zoneSopMap(zoneStr) :+ sequenceOfOpsInZone) )
              }
              else {
                zoneSopMap += (zoneStr ->  List(sequenceOfOpsInZone) )
              }
            }
            activeZoneList -=  zoneStr*/
          }

          else if (command.startsWith("WaitSignal AllocateStation")) {
            val zoneIndex = command.indexOf("Station")
            val zoneStr = cleanName(command.substring(zoneIndex), true)
            if(currentCase.length > 0){
              allocatedInCase = true
            }

            activeZoneList += zoneStr+currentCase
          }

          else if (command.startsWith("WaitSignal ReleaseStation")) {
            val zoneIndex = command.indexOf("Station")
            var zoneStr = cleanName(command.substring(zoneIndex), true)
            var containsCases = false
            var zoneMappingTmp = LinkedHashMap[Operation, Set[String]]()

            // Add all operations that are mapped to this Zone into a ZoneSOP
            // Use the same SOP for all operations concerning the same Zone, but separate them with Arbitrary

            val zones = zoneMapping.map { case (o, zones) => zones }.flatten.toSet
            if (zones.contains(zoneStr)) {

              var zoneSOPSeq = Seq[SOP]()
              var opsInZone = zoneMapping.filter { case (o, zones) => zones.contains(zoneStr) }.map(_._1)

              opsInZone.foreach(opInZone => {
                zoneMapping(opInZone) -= zoneStr
              })

              zones.foreach(zone =>
                if(zone == zoneStr){

                }
                else if(zone.startsWith(zoneStr)){
                  containsCases = true
                  zoneSOPSeq = Seq[SOP]()
                  var opsInZoneCase = zoneMapping.filter{case (o, zones) => zones.contains(zone)}.map(_._1)

                  opsInZoneCase.foreach(opInZone => {
                    zoneMapping(opInZone) -= zoneStr
                  })

                  var opsInZoneWithCases = opsInZoneCase ++  opsInZone
                  //TODO: add logic to decide which order the sops will be in
                  if(allocatedInCase){
                    opsInZoneWithCases = opsInZoneCase ++ opsInZone
                  }

                  opsInZoneWithCases.foreach(opInZone => zoneSOPSeq :+= SOP(Hierarchy((opInZone.attributes.getAs[ID]("newID").get))))
                  var sequenceOfOpsInZone = (Sequence(SOP()) <-- zoneSOPSeq)

                  var zoneSOP = SOP()

                  if (zoneSopMap.contains(zoneStr)) {
                    zoneSopMap += (zoneStr -> (zoneSopMap(zoneStr) :+ sequenceOfOpsInZone))
                  }
                  else {
                    zoneSopMap += (zoneStr -> List(sequenceOfOpsInZone))
                  }
                }
              )
              if (!containsCases){
                zoneSOPSeq = Seq[SOP]()
                opsInZone.foreach(opInZone => zoneSOPSeq :+= SOP(Hierarchy((opInZone.attributes.getAs[ID]("newID").get))))
                var sequenceOfOpsInZone = (Sequence(SOP()) <-- zoneSOPSeq)

                var zoneSOP = SOP()

                if (zoneSopMap.contains(zoneStr)) {
                  zoneSopMap += (zoneStr -> (zoneSopMap(zoneStr) :+ sequenceOfOpsInZone))
                }
                else {
                  zoneSopMap += (zoneStr -> List(sequenceOfOpsInZone))
                }
              }
            }
            else{
              activeZoneList.foreach(activeZone => {
                zoneMapping.foreach(zoneMap =>{
                })
              })

              zones.foreach(zone =>
                if(zone.startsWith(zoneStr+currentCase)){
                  //zoneStr = zoneStr+currentCase
                  var zoneSOPSeq = Seq[SOP]()
                  var opsInZone = zoneMapping.filter { case (o, zones) => zones.contains(zone) }.map(_._1)
                  //opsInZone.foreach(op => zoneMappingTmp += (op -> zones))
                  opsInZone.foreach(opInZone => {zoneMapping(opInZone)  -=   zoneStr})

                  opsInZone.foreach(opInZone => zoneSOPSeq :+=  SOP(Hierarchy((opInZone.attributes.getAs[ID]("newID").get))))
                  var sequenceOfOpsInZone =  (Sequence(SOP()) <-- zoneSOPSeq)

                  var zoneSOP = SOP()

                  if (zoneSopMap.contains(zoneStr)) {
                    //if(!zoneSopMap(zoneStr).contains(sequenceOfOpsInZone)) {
                    zoneSopMap += (zoneStr -> (zoneSopMap(zoneStr) :+ sequenceOfOpsInZone))
                    // }
                  }
                  else {
                    zoneSopMap += (zoneStr ->  List(sequenceOfOpsInZone) )
                  }
                }
              )
            }
            /*activeZoneList.foreach(activeZone =>
              if(activeZone.startsWith(zoneStr)){
                activeZoneList -= activeZone
              })*/
            /*zoneMapping.foreach(op =>
              op._2.foreach(zone =>
                if(!zone.equals(zoneStr+currentCase) || currentCase.length == 0){
                  zonesTmp += zone
                }
              )
            )*/
            zoneMapping.foreach(zoneMap => {
              var zonesTmp = Set[String]()
              var opTmp = zoneMap._1
              zoneMap._2.foreach(zone =>{
                if(!zone.equals(zoneStr+currentCase) || currentCase.length == 0){
                  zonesTmp += zone
                }
              }
              )
              zoneMappingTmp += (opTmp -> zonesTmp)
            }
            )
            zoneMapping = zoneMappingTmp
            activeZoneList -=  zoneStr+currentCase

            activeZoneList.foreach(activeZone => {
              zoneMapping.foreach(zoneMap =>{
                if(zoneMap._2.contains(activeZone)){
                }
              })
            })
            /* val zoneIndex = command.indexOf("Station")
             val zoneStr = cleanName(command.substring(zoneIndex), true)
             // Add all operations that are mapped to this Zone into a ZoneSOP
             // Use the same SOP for all operations concerning the same Zone, but separate them with Arbitrary

             val zones = zoneMapping.map { case (o, zones) => zones }.flatten.toSet
             if (zones.contains(zoneStr)) {
               var zoneSOPSeq = Seq[SOP]()
               var opsInZone = zoneMapping.filter { case (o, zones) => zones.contains(zoneStr) }.map(_._1)

               opsInZone.foreach(opInZone => {zoneMapping(opInZone)  -=   zoneStr})

               opsInZone.foreach(opInZone => zoneSOPSeq :+=  SOP(Hierarchy((opInZone.attributes.getAs[ID]("newID").get))))
               var sequenceOfOpsInZone =  (Sequence(SOP()) <-- zoneSOPSeq)

               var zoneSOP = SOP()

               if (zoneSopMap.contains(zoneStr)) {zoneSopMap += (zoneStr ->  (zoneSopMap(zoneStr) :+ sequenceOfOpsInZone) )}
               else {zoneSopMap += (zoneStr ->  List(sequenceOfOpsInZone) )}
             }
             activeZoneList -=  zoneStr*/
          }


          // PLC signals
            /*
          else if (command.startsWith("WaitSignal")) {
            println("---------------------------Test10----------------------")
            val signalIndex = "WaitSignal".size +1
            val signalName = cleanName(command.substring(signalIndex), false)
            //println("PLC signal is: "+signalName+" and activeZoneList Contains")
            activeZoneList.foreach(activeZone => println(activeZone))

            var opId = ID.newID
            val newOp = Operation(robotSchedule + "_" + signalName, List(), SPAttributes("robotSchedule" -> robotSchedule, "original" -> opId, "newID" -> opId), opId)

            zoneMapping += (newOp -> activeZoneList)
            collector.opWithID(newOp.name, Seq(newOp.attributes merge h), opId)
            operationList :+= newOp


            var allOperationSequencesCopy = List[List[Operation]]()
            allOperationSequences.foreach(OpSeq => allOperationSequencesCopy :+= OpSeq)
            allOperationSequences = List[List[Operation]]()


            val nrOfOperationSequences = allOperationSequencesCopy.size
            if (nrOfOperationSequences != 0) {
              for {i <- 0 until nrOfOperationSequences}
                yield {
                  allOperationSequences :+= allOperationSequencesCopy(i) :+ newOp
                }
            }
            else
              allOperationSequences :+=  List(newOp)


            ss = List(if (!ss.isEmpty) Sequence(SOP()) <-- (ss(0).sop :+ SOP(Hierarchy((newOp.attributes.getAs[ID]("newID").get)))) else Sequence(SOP(Hierarchy((newOp.attributes.getAs[ID]("newID").get))) ))
            println("---------------------------Test11----------------------")
          }*/

          // Check if any of the "Commands" contains a string with a Case statement.
          else if (command.startsWith("WaitCase")) {
            val CaseSeparatedInAlphaNumericalList = command.split("\\W+")
            // Take the case statement and split it into parts of alphanumerical symbols
            var caseSopSeqs = List[SOP]()
            var activeZoneListtmp = List[Set[String]]()
            var zoneMappingListTmp = List[LinkedHashMap[Operation, Set[String]]]()
            var sstemp = List[List[SOP]]()
            var zoneListTmp = Set[String]()
            var zoneMappingCase = LinkedHashMap[Operation, Set[String]]()
            var tmpZoneMapping = LinkedHashMap[Operation, Set[String]]()
            var nrOfCases = 0

            var allOperationSequencesCopy = List[List[Operation]]()
            allOperationSequences.foreach(OpSeq => allOperationSequencesCopy :+= OpSeq)
            allOperationSequences = List[List[Operation]]()



            CaseSeparatedInAlphaNumericalList.foreach(caseString =>
              availableOperations.find(operation => operation.name == caseString)
              match{
                case Some(operation) =>
                  nrOfCases += 1
                case None =>
              }
            )
            // TODO Fix so that we add the caseStrings before
            zoneMapping.foreach(zoneMap =>{
              var containsActiveZone = false
              zoneListTmp = Set[String]()
              //println("zones in activeZonelist")
              zoneMap._2.foreach(zone =>{
                //println(activeZone)
                if(activeZoneList.contains(zone)){
                  for(i <- 1 to nrOfCases){
                    zoneListTmp += zone+"Case"+i.toString
                  }
                }
                else{
                  zoneListTmp += zone
                }

              })
              tmpZoneMapping += (zoneMap._1 -> zoneListTmp)
              /* if(containsActiveZone){

                 //zoneMappingCase += (zoneMap._1 -> zoneMap._2)
               }
               else{
                 tmpZoneMapping += (zoneMap._1 -> zoneMap._2)
               }*/
            })
            CaseSeparatedInAlphaNumericalList.foreach(caseString => // Go through each case part and check if the part exists as an operation in the availableOps tree.

              availableOperations.find(operation => operation.name == caseString)
              match {
                case Some(operation) =>
                  var zoneMappingtmp = LinkedHashMap[Operation, Set[String]]()
                  zoneMappingtmp ++= tmpZoneMapping
                  // Copy the info from zoneMapping
                  val operationChildIds = hierarchyRoot.getChildren(operation.id)
                  var robotCommandstmp = List[String]()
                  var newOp: Operation = operation
                  var allOperationSequencesTmp = List[List[Operation]]()

                  operationChildIds.foreach(opChild => ops.find(op => op.id == opChild)
                  match {
                    case Some(op) =>
                      if (op.name == caseString) {
                        robotCommandstmp = op.attributes.getAs[List[String]]("robotcommands").getOrElse(List())

                        var opId = ID.newID

                        newOp = op.copy(name = robotSchedule + "_" + op.name, attributes = op.attributes merge
                          SPAttributes("robotSchedule" -> robotSchedule, "original" -> operation.id, "newID" -> opId))
                        // Create temperary zones and stations for the different alternatives if the zone is allocated before the branche
                        var caseStringTmp = ""
                        if (activeZoneList.nonEmpty) {
                          var caseStringIndexTmp = op.name.indexOf("Case")
                          caseStringTmp = cleanName(op.name.substring(caseStringIndexTmp), false)
                          caseListTmp :+= caseStringTmp
                          zoneListTmp = Set[String]()
                          activeZoneList.foreach(activeZone =>{
                            // If this makes it worse go back to if(true)
                            if(!zoneListTmp.contains(activeZone)/*If zoneListTmp dont't contains active zone*/) {
                              zoneListTmp += activeZone + caseStringTmp
                            }
                          })
                          /*zoneMapping.foreach(zoneMap =>{
                            var containsActiveZone = false
                            var tmpZones = Set[String]()
                            activeZoneList.foreach(activeZone =>{
                              if(zoneMap._2.contains(activeZone)){
                                containsActiveZone = true
                                tmpZones += activeZone+caseStringTmp
                              }
                              else{
                                tmpZones += activeZone
                              }
                            })
                            if(containsActiveZone){
                              zoneMappingtmp += (zoneMap._1 -> tmpZones)
                            }
                          })*/
                        }
                        //zoneMappingtmp += (newOp -> activeZoneList)
                        zoneMappingtmp += (newOp -> zoneListTmp)
                        collector.opWithID(newOp.name, Seq(newOp.attributes merge h), opId)
                        operationList :+= newOp
                      }
                      availableOperations ::= op
                    case None =>
                  }
                  )
                  //Get next case
                  if (caseListTmp.size > 0) {
                    currentCase = caseListTmp(0)
                    caseListTmp = caseListTmp.tail
                  }

                  if (!robotCommandstmp.isEmpty) {
                    plcSignalsStartConditions = plcSignalsStartConditions + ""
                    // map the signals to the robotcommands that are to be executed.
                    activeZoneListtmp.foreach(activeZonestmp => {
                    })
                    var tmp = splitIntoOpsAndZonesNew(zoneMappingtmp, zoneSopMap, operationList, /*activeZoneList*/ zoneListTmp, robotCommandstmp, availableOperations, plcSignalsStartConditions, robotSchedule, hierarchyRoot, ops, collector, h, currentCase)
                    zoneMappingListTmp :+= tmp._1;
                    operationList = tmp._2;
                    activeZoneListtmp :+= tmp._3;
                    caseSopSeqs = tmp._4;
                    zoneSopMap = tmp._6;
                    allOperationSequencesTmp = tmp._7;
                    currentCase = ""
                    activeZoneListtmp.foreach(activeZonestmp => {
                      activeZonestmp.foreach(activeZone => {

                      })
                    })

                  }
                  else {
                    zoneListTmp.foreach(activeZone => {
                      zoneMappingtmp.foreach(zoneMap =>{
                        if(zoneMap._2.contains(activeZone)){
                        }
                      })
                    })
                    activeZoneListtmp.foreach(activeZonestmp => {
                      activeZonestmp.foreach(activeZone => {
                      })
                    })
                    activeZoneListtmp :+= zoneListTmp //activeZoneList
                    activeZoneListtmp.foreach(activeZonestmp => {
                      activeZonestmp.foreach(activeZone => {

                      })
                    })
                    zoneMappingListTmp :+= zoneMappingtmp
                    // ???
                    allOperationSequencesTmp
                  }
                  activeZoneListtmp.foreach(activeZonestmp =>
                  {activeZonestmp.foreach(activeZone => {
                    zoneMappingtmp.foreach(zoneMap =>{
                    })
                  })

                  })
                  caseSopSeqs = if (!caseSopSeqs.isEmpty) List(Sequence(SOP(Hierarchy((newOp.attributes.getAs[ID]("newID").get)))) ++ caseSopSeqs(0).sop) else List(Sequence(SOP(Hierarchy((newOp.attributes.getAs[ID]("newID").get)))))
                  sstemp :+= caseSopSeqs


                  val nrOfOperationSequences = allOperationSequencesCopy.size

                  if (allOperationSequencesTmp.size != 0) {
                    allOperationSequencesTmp.foreach(opSeqTmp => {
                      if (nrOfOperationSequences != 0) {
                        for {i <- 0 until nrOfOperationSequences}
                          yield {
                            allOperationSequences :+= (allOperationSequencesCopy(i) :+ newOp) ++ opSeqTmp
                          }
                      }
                      else
                        allOperationSequences :+= List(newOp) ++ opSeqTmp
                    }
                    )
                  }
                  else
                    allOperationSequences :+= List(newOp)


                case none =>
              })
            //Maybe can be removed
            currentCase = ""
            var sequenceOfSops = Seq[SOP]()
            sstemp.foreach(sslist => {
              if (!sslist.isEmpty) {
                sequenceOfSops :+= (Sequence(SOP()) <-- sslist(0).sop)
              }
            })

            var activeZonestmp = Set[String]()
            activeZoneListtmp.foreach(activeZoneSet => activeZonestmp = activeZonestmp.union(activeZoneSet))
            activeZoneList = activeZonestmp

            zoneMappingListTmp.foreach(zoneMap => zoneMapping ++= zoneMap)

            if (!sequenceOfSops.isEmpty) {
              var AlternativeSeqs = Alternative(SOP()) <-- sequenceOfSops
              ss = List(if (!ss.isEmpty) Sequence(SOP()) <-- (ss(0).sop :+ AlternativeSeqs) else Sequence(AlternativeSeqs))
            }

            //allocatedInCase = false

          }
          else if (command.startsWith("!")) {
          } // do nothing
          else {
            val cleanOpName = cleanName(command, false)
            //println("Current command: "+cleanOpName+" and currentCase is: "+currentCase)
            availableOps.find(operation => operation.name == cleanOpName) match {
              // find the current command string in the list of available operations.
              case Some(operation) => // If the operation exists:
                // operation o needs the currently active zones
                val opId = ID.newID
                val newOp = operation.copy(name = robotSchedule + "_" + operation.name, attributes = operation.attributes merge
                  SPAttributes("robotSchedule" -> robotSchedule, "original" -> operation.id, "newID" -> opId))

                zoneMapping += (newOp -> activeZoneList)
                collector.opWithID(newOp.name, Seq(newOp.attributes merge h), opId)
                operationList :+= newOp
                //println("operationList    ::::  "    + operationList )

                var allOperationSequencesCopy = List[List[Operation]]()
                allOperationSequences.foreach(OpSeq => allOperationSequencesCopy :+= OpSeq)
                allOperationSequences = List[List[Operation]]()


                val nrOfOperationSequences = allOperationSequencesCopy.size
                if (nrOfOperationSequences != 0) {
                  for {i <- 0 until nrOfOperationSequences}
                    yield {
                      allOperationSequences :+= allOperationSequencesCopy(i) :+ newOp

                    }
                }
                else
                  allOperationSequences :+=  List(newOp)

                ss = List(if (!ss.isEmpty) Sequence(SOP()) <-- (ss(0).sop :+ SOP(Hierarchy((newOp.attributes.getAs[ID]("newID").get)))) else Sequence(SOP(Hierarchy((newOp.attributes.getAs[ID]("newID").get))) ))
              case none =>
            }
          }
        })
        (zoneMapping, operationList, activeZoneList, ss, plcSignalsStartConditions, zoneSopMap, allOperationSequences)
      }




      // a function that creates an alternative of all provided operations in a hierarchy
      def fixtureAndTTSopsAndOpsNew( topMostOpsInHierarchy: List[Operation], robotSchedule: String,
                                  hierarchyRoot: HierarchyRoot, allOps: List[Operation], collector: CollectorModel, h: SPAttributes )
      :  (LinkedHashMap[Operation, Set[String]],List[Operation], Set[String], List[SOP], String, String, String, Map[String, Seq[SOP]], List[List[Operation]]) = {

        val zoneMap = LinkedHashMap[Operation, Set[String]]()
        var operationList = List[Operation]()
        val activeZoneList = Set[String]()
        var ss = List[SOP]()
        var startCondition = "idle"
        var endCondition = "idle"
        var plcSignalsStartConditions = ""
        val zoneSopMap = Map[String, Seq[SOP]]()
        var allOperationSequences = List[List[Operation]]()
        var opIdList= List[ID]()


        val numberOfOPs = topMostOpsInHierarchy.size
        val outgoing_links = List.fill(numberOfOPs)("")
        val initOpID = ID.newID
        val initOP = Operation("firstOp",List(),SPAttributes(),initOpID)
        val newInitOP = initOP.copy(name = robotSchedule + "_" + initOP.name, attributes = initOP.attributes merge
          SPAttributes("robotSchedule" -> robotSchedule, "original" -> initOP.id, "newID" -> initOpID, "incoming_links"-> List(), "outgoing_links" -> outgoing_links))

        endCondition = newInitOP.name + "_done"

        val rs = newInitOP.attributes.getAs[String]("robotSchedule").getOrElse("error")
        val trans = SPAttributes(collector.aResourceTrans(robotScheduleVariable(rs), startCondition, newInitOP.name, endCondition))
        collector.opWithID(newInitOP.name, Seq(newInitOP.attributes merge trans merge h), initOpID)

        operationList :+= newInitOP

        startCondition = endCondition

        topMostOpsInHierarchy.foreach(operation => {

          var opNameCleaned = operation.name.replace('-', '_') // the synthesis is unable to handle "-" for some reason.
          val newOpID = ID.newID
          val newOp = operation.copy(name = robotSchedule + "_" + opNameCleaned, attributes = operation.attributes merge
            SPAttributes("robotSchedule" -> robotSchedule, "original" -> operation.id, "newID" -> newOpID))

          val rs = newOp.attributes.getAs[String]("robotSchedule").getOrElse("error")
          val trans = SPAttributes(collector.aResourceTrans(robotScheduleVariable(rs), startCondition, newOp.name, "idle"))
          collector.opWithID(newOp.name, Seq(newOp.attributes merge trans merge h), newOpID)

          operationList :+= newOp
          allOperationSequences :+= List(newOp)

          opIdList :+= newOpID
        })
        var newOpSeq = List(newInitOP)
        var newAllOperationSequences = List[List[Operation]]()
        allOperationSequences.foreach(opSeq => newAllOperationSequences :+= newOpSeq :+ opSeq(0) )

        ss = List(Sequence(Hierarchy(initOpID),Alternative(opIdList.map(id=>Hierarchy(id)):_*)))

        (zoneMap, operationList, activeZoneList, ss, startCondition, endCondition, plcSignalsStartConditions, zoneSopMap, newAllOperationSequences)
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

      def findParentString(id: String, node: HierarchyNode): (Option[HierarchyNode],Boolean) = {
        if(node.children.exists(_.item.toString == id)) (Some(node),true)
        else {
          val res = node.children.map(findParentString(id,_)._1).flatMap(x=>x)
          if(res.isEmpty) (None,false)
          else (Some(res.head),true)
        }
      }


        def robotScheduleVariable(rs: String) = "v"+rs+"_pos"


    //-------------------------------------------------------------------------------------------------------------------------
    case _ => sender ! SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }

}



