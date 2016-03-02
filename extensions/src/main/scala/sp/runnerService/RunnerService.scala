package sp.runnerService

import akka.actor._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._
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
import sp.domain.Logic._
import sp.extensions._
import sp.psl.{ModelMaking, PSLModel}
import scala.util.Try
import sp.domain.SOP._


object RunnerService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "runner",
      "description" -> "A service to run SOP's in SP"
    ),
    "SOP" -> KeyDefinition("Option[ID]", List(), Some("")
    )
  )

  val transformTuple  = (
    TransformValue("SOP", _.getAs[ID]("SOP"))
    )
  val transformation = List(transformTuple)
  //def props(eventHandler: ActorRef) = Props(classOf[RunnerService], eventHandler)
  def props(eventHandler: ActorRef, operationController: String) =
    ServiceLauncher.props(Props(classOf[RunnerService], eventHandler, operationController))
}

// Inkluderar eventHandler och namnet på servicen operationController. Skickas med i SP.scala
class RunnerService(eventHandler: ActorRef, operationController: String) extends Actor with ServiceSupport {
  import context.dispatcher

  var parents: Map[SOP, SOP] = Map()
  var activeSteps: List[SOP] = List()
  var parallelRuns: Map[Parallel, List[SOP]] = Map()
  var state: State = State(Map())


  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you whant to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      val sopID = transform(RunnerService.transformTuple)

      //lista av tuplar som vi gör om till map
      val idMap: Map[ID, IDAble] = ids.map(item => item.id -> item).toMap
      val sop = Try{idMap(sopID).asInstanceOf[SOP]}

      // Makes the parentmap
      sop.foreach(createSOPMap)

      // Starts the first op
      sop.foreach(executeSOP)



    }
      // Vi får states från Operation control
    case r @ Response(ids, attr, service, _) if service == operationController => {
      // Till att börja med är dessa tomma, så vi säger att alla som kör blir färdiga
      val res = activeSteps.map(stepCompleted)

      if (res.foldLeft(false)(_ || _)){

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
    sop match {
      case x: Parallel => x.sop.foreach(executeSOP)
      case x: Sequence if x.sop.nonEmpty => executeSOP(x.sop.head)
      case x: Hierarchy => {
        startID(x.operation)
        activeSteps = activeSteps :+ x
      }
      case x => println(s"Hmm, vi fick $x i executeSOP")
    }
  }

  import scala.concurrent._
  import scala.concurrent.duration._
  def startID(id: ID) = {
    // Skickar ett tomt svar efter 2s.
    context.system.scheduler.scheduleOnce(2000 milliseconds, self,
      Response(List(),
        SPAttributes("state"-> SPAttributes()),
        operationController,
        ID.newID
      ))
  }


  // Anropas när ett steg blir klart
  def stepCompleted(complSOP: SOP) = {
    parents.get(complSOP) match {
      case Some(p: Parallel) => {
        val alreadyDoneSteps = parallelRuns.get(p)
        if (alreadyDoneSteps.isEmpty)
          parallelRuns = parallelRuns + (p->List(complSOP))

        // Om alla är färdiga -> stepCompleted(p)
        // annars vänta
        false
      }
      case Some(p: Sequence) => {
        // plocka nästa ur sekvensen och kör -> execute(nextSOP)
        // uppdatera activeSteps
        // om det var sista steget -> stepCompleted(p)
        false
      }
      case None => {
        // nu är vi färdiga
        true
      }
    }

  }







}


trait SOPRunner {
  var parents: Map[SOP, SOP] = Map()
  var activeSteps: List[SOP] = List()
  var parallelRuns: Map[Parallel, List[SOP]]



}




