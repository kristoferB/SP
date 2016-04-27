package sp.runnerService

import akka.actor._
import akka.util.Timeout
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._
import org.json4s.JsonAST.JInt
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.logic
import sp.domain.Logic._
import sp.extensions._
import sp.psl._
import scala.util.Try
import sp.domain.SOP._
import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.Props

case class AbilityStructure(name: String, parameter: Option[(String, Int)])

object RunnerService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "runner",
      "description" -> "A service to run SOP's in SP"
    ),
    "SOP" -> KeyDefinition("Option[ID]", List(), Some("")
    )
  )

  val transformTuple  = TransformValue("SOP", _.getAs[ID]("SOP"))

  val transformation = List(transformTuple)
  //def props(eventHandler: ActorRef) = Props(classOf[RunnerService], eventHandler)
  def props(eventHandler: ActorRef, serviceHandler: ActorRef, operationController: String) =
    ServiceLauncher.props(Props(classOf[RunnerService], eventHandler, serviceHandler, operationController))
}

// Inkluderar eventHandler och namnet på servicen operationController. Skickas med i SP.scala
class RunnerService(eventHandler: ActorRef, serviceHandler: ActorRef, operationController: String) extends Actor with ServiceSupport {
  import context.dispatcher

  var parents: Map[SOP, SOP] = Map()
  var fakeParents: Map[SOP, SOP] = Map()
  var activeSteps: List[Hierarchy] = List()
  var parallelRuns: Map[Parallel, List[SOP]] = Map()
  var state: State = State(Map())
  var reply: Option[RequestNReply] = None
  var readyList: List[ID] = List()
  var sopen: Option[SOP] = None
  var operationAbilityMap = Map[ID, AbilityStructure]() // fejk id till abilityStructure
  var abilityMap = Map[String, Operation]()             // namn till äkta operation
  var realToFakeID: Map[ID, ID] = Map()
  var parameterMap: Map[String, ID] = Map()
  implicit var rnr: RequestNReply = null
  implicit val timeout = Timeout(2 seconds)




  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      rnr = RequestNReply(r, replyTo)
      reply = Some(rnr)

      // Lyssna på events från alla
      eventHandler ! SubscribeToSSE(self)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      val sopID = transform(RunnerService.transformTuple)

      //lista av tuplar som vi gör om till map
      val idMap: Map[ID, IDAble] = ids.map(item => item.id -> item).toMap
      val sop = Try{idMap(sopID).asInstanceOf[SOPSpec].sop}.map(xs => Parallel(xs:_*))

      val ops = ids.collect{case o:Operation => o}
      operationAbilityMap = ops.flatMap{ o =>
        o.attributes.getAs[AbilityStructure]("ability").map(o.id -> _)
      }.toMap

      val things = ids.collect{case t: Thing => t}
      parameterMap = things.map(t => t.name -> t.id).toMap

      val abilities = ops.collect{case o: Operation if o.attributes.getAs[String]("operationType").getOrElse("not") == "ability" => o}
      println("abilities = " + abilities)
      abilityMap = abilities.map(o => o.name+".run" -> o).toMap

      val keys = abilityMap.keySet
      operationAbilityMap.foreach{ case (fakeID, abStruct) =>
        if(keys.contains(abStruct.name)) {
          val realID = abilityMap.get(abStruct.name).get.id
          realToFakeID = realToFakeID + (realID -> fakeID)
        }
      }


      //println("abilityMap created with" + abilityMap.keySet.head)

      askAService(Request(operationController, SPAttributes("command"->SPAttributes("commandType"->"status"))),serviceHandler)

      println(s"operationAbilityMap is: $operationAbilityMap")
      println(s"abilityMap is: $abilityMap")


      // Makes the parentmap
      sop.foreach(createSOPMap)
      sopen = sop.toOption
      println("we got a sop: "+sop)

      // Starts the first op
      //      sop.foreach(executeSOP)
      progress ! SPAttributes("activeOps"->activeSteps)
    }

    // Vi får states från Operation control
    case r @ Response(ids, attr, service, _) if service == operationController => {
      println(s"we got a state change")

      val newState = attr.getAs[State]("state")
      newState.foreach{s =>
        state = State(state.state ++ s.state.filter{case (id, v)=>
          state.get(id) != newState.get(id)
        })}

      //lägger till alla ready states i en readyList och tar bort gamla som inte är ready längre

      val readyStates = state.state.filter{case (id, value) =>
        value == SPValue("ready")
      }
      readyList = readyStates.keys.toList
      println(s"readyList: $readyList")
      println(s"state: $state")

      // if there is nothing started yet
      if(activeSteps.isEmpty) {
        sopen.foreach(executeSOP)
        println("after first new state, started execution")
      } else {
        val completedIDs = state.state.filter{case (i,v) => v == SPValue("completed")}.keys.toList
        println("completed ids = " + completedIDs)
        // find which "activeSteps" the ids correspond to
        val activeCompleted = activeSteps.filter(x=>completedIDs.contains(x.operation))
        println("Från PLC : det som finns i activeSteps" + activeSteps)
        println("de aktiva som vi lagt till och är klara: " + activeCompleted)
        println()
        // execute completed to flop run bit
        activeCompleted.foreach(x=>startID(x.operation))
        println("Försöker köra de igen")
        println()
        // remove the completed ids
        activeSteps = activeSteps.filterNot(x=>completedIDs.contains(x.operation))
        println(s"Från PLC : Activesteps contains: $activeSteps")
        println("Från PLC : Sätter de som är helt klara genom stepCompleted")
        val res = activeCompleted.map(stepCompleted)
        // Kolla om hela SOPen är färdigt. Inte säker på att detta fungerar
        if (res.foldLeft(false)(_ || _)){
          println("RunnerService: All done")
          reply.foreach(rnr => rnr.reply ! Response(List(), SPAttributes("status"->"done", "silent"->true), rnr.req.service, rnr.req.reqID))
          self ! PoisonPill
        }
      }
    }
  }

  def createSOPMap(x: SOP): Unit = {
    println("createSOPMap")
    x.sop.foreach {
      case c: Hierarchy =>
        val abs = operationAbilityMap.get(c.operation).get
        val a = abilityMap.get(abs.name).get
        val h = Hierarchy(a.id)
        println(s"adding in hierarchy : $h to $x")
        parents = parents + (h -> x)
        fakeParents = fakeParents + (h -> c)
      case c => parents = parents + (c -> x)
        println(s"adding regular : $c to $x")
        createSOPMap(c) // terminerar när en SOP inte har några barn
    }
  }

  def executeSOP(sop: SOP): Unit = {
    if (sop.isInstanceOf[Hierarchy]) println(s"executing sop $sop")
    sop match {
      case x: Parallel => x.sop.foreach(executeSOP)
      case x: Sequence if x.sop.nonEmpty => executeSOP(x.sop.head)
      case x: Hierarchy => {
        val abs = operationAbilityMap.get(x.operation).get
        println("abs = " + abs)
        println("abs name = " + abs.name)
        println("abilitymap =" + abilityMap)
        val a = abilityMap.get(abs.name).get
        println("a = " + a)
        println("a =" + a)
        if (checkPreCond(a)) {
          println("starting operation= " + a.id)
          startID(a.id)
          activeSteps = activeSteps :+ Hierarchy(a.id)
          println(s"Started ${a.id}, activeSteps: $activeSteps")
        } else {
          println("precondition failed")
        }
      }
      case x => println(s"Hmm, vi fick $x i executeSOP")
    }
  }


  // kollar om en operations alla preconditions är uppfyllda och kan köras
  def checkPreCond(x: Operation): Boolean = {
    val temp = x.conditions.collect{case pc: PropositionCondition => pc}
    val enabled = temp.foldLeft(true)((a, b) => a && b.eval(state))
    println(s"checking precondition, conditions = $enabled" )

    println(s"checkPreCond : readylist: $readyList")
    println(s"CheckPreCond : op: ${x.id}")

    println("We got: " + (readyList.contains(x.id) && enabled))
    readyList.contains(x.id) && enabled
  }


  def startID(id: ID) = {
    println(s"startID : starting id $id")
    val fakeID = realToFakeID.get(id).get
    val abStructToFake = operationAbilityMap.get(fakeID).get
    val paraMap: Map[ID,SPValue] = abStructToFake.parameter match {
      case None => Map()
      case Some((str, num)) =>
        val paraID = parameterMap.get(str).get
        Map(paraID -> SPValue(num))
    }

    println(s"paraMap is: $paraMap")

    val attr = SPAttributes("command"->SPAttributes("commandType"->"execute", "execute"->id,
      "parameters" -> State(paraMap)))

    askAService(Request(operationController, attr), serviceHandler)
  }


  // Anropas när ett steg blir klart
  def stepCompleted(complSOP: SOP): Boolean = {
    println(s"step $complSOP is completed. Parent is ${parents.get(complSOP)}")
    parents.get(complSOP) match {
      case Some(p: Parallel) => {
        if (parallelRuns.get(p).isEmpty)
          parallelRuns = parallelRuns + (p->List()) // lägger till nuvarande sop i en lista med p som key
        parallelRuns = parallelRuns + (p->(parallelRuns(p) :+ complSOP))
        if(p.sop.length > parallelRuns(p).size) {
          false
        }
        else       // om alla i parallellen är klara anropas stepComp igen med p
          stepCompleted(p)
        // Om alla är färdiga -> stepCompleted(p)
        // annars vänta
      }
      case Some(p: Sequence) => {
        val parentSeq = p.sop                   // ger hela föräldern i en seq
        val nbrOfChildren = parentSeq.length
        var current = -10

        if(complSOP.isInstanceOf[Hierarchy]) {
          complSOP.asInstanceOf[Hierarchy]
          val newSOP = fakeParents.get(complSOP).get
          current = parentSeq.indexOf(newSOP)
        } else {
          current = parentSeq.indexOf(complSOP)
        }

        println(s"current = $current")
        if ( current == -10) {
          println("ngt gick väldigt fel.. ")
          false
        } else if (current >= nbrOfChildren-1) {     // om det är sista steget i sekvensen -> stepCompleted(p)
          stepCompleted(p)
        }
        else {     // om nuvarande index är mindre än antalet barn tas nuvarande bort ur lista
          // över aktiva och nästkommande steg i seq startar
          executeSOP(parentSeq(current + 1))
          false
        }
      }
      case None => {
        // nu är vi färdiga
        true
      }
      case x@_ => {
        println(s"stepcompleted: got $x")
        false
      }
    }
  }
}






