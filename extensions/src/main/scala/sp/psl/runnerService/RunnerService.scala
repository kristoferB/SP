package sp.psl.runnerService

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

//case class AbilityStructure(name: String, parameter: Option[(String, Int)])

object RunnerService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "runner",
      "description" -> "A service to run SOP's in SP"
    ),
      "SOP" -> KeyDefinition("Option[ID]", List(), Some("")),
      "station" -> KeyDefinition("String", List(), Some("noStation")
    )
  )

  val transformTuple  = (TransformValue("SOP", _.getAs[ID]("SOP")), TransformValue("station", _.getAs[String]("station")))

  val transformation = transformToList(transformTuple.productIterator.toList)
  def props(eventHandler: ActorRef, serviceHandler: ActorRef, operationController: String) =
    ServiceLauncher.props(Props(classOf[RunnerService], eventHandler, serviceHandler, operationController))
}

// Inkluderar eventHandler och namnet på servicen operationController. Skickas med i SP.scala
class RunnerService(eventHandler: ActorRef, serviceHandler: ActorRef, operationController: String) extends Actor with ServiceSupport {
  import context.dispatcher

  var parents: Map[SOP, SOP] = Map()
  var activeSteps: List[Hierarchy] = List()
  var parallelRuns: Map[Parallel, List[SOP]] = Map()
  var state: State = State(Map())
  var reply: Option[RequestNReply] = None
  var readyList: List[ID] = List()
  var sopen: Option[SOP] = None
  var operationAbilityMap = Map[ID, AbilityStructure]() // operation ID to AbilityStructure
  var abilityMap = Map[ID, Operation]()             // Ability name to ability (operation)
  var parameterMap: Map[String, ID] = Map()
  var station: String = ""
  var progress: ActorRef = self
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
      progress = context.actorOf(progressHandler)

      val sopID = transform(RunnerService.transformTuple._1)
      station = transform(RunnerService.transformTuple._2)

      //list of tuples into maps
      val idMap: Map[ID, IDAble] = ids.map(item => item.id -> item).toMap
      val sop = Try{idMap(sopID).asInstanceOf[SOPSpec].sop}.map(xs => Parallel(xs:_*))

      // maps all operation ids to an AbilityStructure
      val ops = ids.collect{case o:Operation => o}
      operationAbilityMap = ops.flatMap{ o =>
        o.attributes.getAs[AbilityStructure]("ability").map(o.id -> _)
      }.toMap

      // saves all parameters into a map
      val things = ids.collect{case t: Thing if t.name.endsWith(".pos")  => t}
      parameterMap = things.map(t => t.name -> t.id).toMap

      // maps all names of abilities to the ability id
      val abilities = ops.collect{case o: Operation if o.attributes.getAs[String]("operationType").getOrElse("not") == "ability" => o}
      println("abilities = " + abilities)
      abilityMap = abilities.map(o => o.id -> o).toMap

      askAService(Request(operationController, SPAttributes("command"->SPAttributes("commandType"->"status"))),serviceHandler)

      // Makes the parentmap
      sop.foreach(createSOPMap)
      sopen = sop.toOption
      println(s"we got a sop: $sop")

      progress ! SPAttributes("station"->station,"activeOps"->activeSteps)
    }

    // We get states from Operation control
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

      // if there is nothing started yet
      if(activeSteps.isEmpty) {
        sopen.foreach(executeSOP)
        println("activeStep empty -> start executing SOP")
        progress ! SPAttributes("station"->station,"activeOps"->activeSteps)
      } else {
        val completedIDs = state.state.filter{case (i,v) => v == SPValue("completed")}.keys.toList
        println("completed ids = " + completedIDs)

        val opsThatHasCompletedAbilities = (operationAbilityMap.filter{case (o, struct) =>
          val abilityId = struct.id
          completedIDs.contains(abilityId)
        }).keySet

        val activeCompleted = activeSteps.filter(x=>opsThatHasCompletedAbilities.contains(x.operation))

        // execute completed to flop run bit
        activeCompleted.foreach(x=>startID(x.operation))
        // remove the completed ids
        activeSteps = activeSteps.filterNot(x=>opsThatHasCompletedAbilities.contains(x.operation))

        val res = activeCompleted.map(stepCompleted)
        // Kolla om hela SOPen är färdigt. Inte säker på att detta fungerar
        if (res.foldLeft(false)(_ || _)){
          println("RunnerService: All done")
          reply.foreach(rnr => rnr.reply ! Response(List(), SPAttributes("station"->station,"status"->"done", "silent"->true), rnr.req.service, rnr.req.reqID))
          self ! PoisonPill
        } else {
          progress ! SPAttributes("station"->station,"activeOps"->activeSteps)
        }
      }
    }
  }

  def createSOPMap(x: SOP): Unit = {
    println("createSOPMap")
    x.sop.foreach { c =>
      parents = parents + (c -> x)
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
        val a = abilityMap(abs.id)
        if (checkPreCond(a)) {
          startID(x.operation)
          activeSteps = activeSteps :+ x
          println(s"Started ability id ${a.id} with operation id ${x.operation}, activeSteps: $activeSteps")
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
    println("We got: " + (readyList.contains(x.id) && enabled))
    readyList.contains(x.id) && enabled
  }


  def startID(id: ID) = {
    var paraMap: Map[ID, SPValue] = Map()
    val abStructToFake = operationAbilityMap(id)
    paraMap = abStructToFake.parameters.map(p => p.id -> p.value).toMap


    println(s"paraMap is: $paraMap")
    val abID = abStructToFake.id
    val attr = SPAttributes("command"->SPAttributes("commandType"->"execute", "execute"->abID,
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
        else
          stepCompleted(p)
        // Om alla är färdiga -> stepCompleted(p), annars vänta
      }
      case Some(p: Sequence) => {
        val parentSeq = p.sop
        val nbrOfChildren = parentSeq.length
        val current = parentSeq.indexOf(complSOP)

        println(s"current = $current")
         if (current >= nbrOfChildren-1) {     // om det är sista steget i sekvensen -> stepCompleted(p)
          stepCompleted(p)
        } else {
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






