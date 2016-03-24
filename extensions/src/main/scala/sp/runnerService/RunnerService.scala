package sp.runnerService

import akka.actor._
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
  var activeSteps: List[SOP] = List()
  var parallelRuns: Map[Parallel, List[SOP]] = Map()
  var state: State = State(Map())
  var reply: Option[RequestNReply] = None
  var readyList: List[ID] = List()


  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      reply = Some(rnr)

      // Lyssna på events från alla
      eventHandler ! SubscribeToSSE(self)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      val sopID = transform(RunnerService.transformTuple)

      //lista av tuplar som vi gör om till map
      val idMap: Map[ID, IDAble] = ids.map(item => item.id -> item).toMap
      val sop = Try{idMap(sopID).asInstanceOf[SOPSpec].sop}.map(xs => Parallel(xs:_*))


      // Makes the parentmap
      sop.foreach(createSOPMap)
      println("we got a sop: "+sop)


      // Starts the first op
      sop.foreach(executeSOP)
      progress ! SPAttributes("activeOps"->activeSteps)
      startID(ID.newID)
    }

    // Vi får states från Operation control
    case r @ Response(ids, attr, service, _) if service == operationController => {
      println(s"we got a state change")

      val newState = attr.getAs[SPAttributes]("").get.getAs[State]("state").get

      state = State(state.state ++ newState.state.filter{case (id, v)=>
        state.get(id) != newState.get(id)
      })


      //lägger till alla ready states i en readyList och tar bort gamla som inte är ready längre
      state.state.keys.foreach(id => {
        val temp = state.state.get(id).get.toString
        if (!readyList.contains(id) && temp == "ready") {
          readyList :+ id
        } else if(readyList.contains(id) && temp != "ready") {
          readyList.filter(_ != id)
        }
      })


      // Plocka ut alla färdiga steg här
      // Skicka varje färdigt til stepCompl
      val copyAS = activeSteps
      activeSteps = List()
      val res = copyAS.map(stepCompleted)
      startID(ID.newID)


      // Kolla om hela SOPen är färdigt. Inte säker på att detta fungerar
      if (res.foldLeft(false)(_ || _)){
        reply.foreach(rnr => rnr.reply ! Response(List(), SPAttributes("status"->"done"), rnr.req.service, rnr.req.reqID))
        self ! PoisonPill
      }
    }
  }


  def createSOPMap(x: SOP): Unit = {
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
        if (checkPreCond(x)) {
          startID(x.operation)
          activeSteps = activeSteps :+ x
        }
      }
      case x => println(s"Hmm, vi fick $x i executeSOP")
    }
  }


  // kollar om en operations alla preconditions är uppfyllda och kan köras
  def checkPreCond(x: Hierarchy): Boolean = {
    val temp = x.conditions.collect{case pc: PropositionCondition => pc}
    val enabled = temp.foldLeft(true)((a, b) => a && b.eval(state))

    readyList.contains(x.operation) && enabled
  }


  def startID(id: ID) = { askAService(Request(operationController, SPAttributes("command"->SPAttributes("execute"->id))),serviceHandler) }


  // Anropas när ett steg blir klart
  def stepCompleted(complSOP: SOP): Boolean = {
    //println(s"step $complSOP is completed. Parent is ${parents.get(complSOP)}")
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
        val current = parentSeq.indexOf(complSOP)

        if (current >= nbrOfChildren-1) {     // om det är sista steget i sekvensen -> stepCompleted(p)
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
    }
  }
}






