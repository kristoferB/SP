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
import sp.domain.Logic._
import sp.extensions._
import sp.psl._
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

  val transformTuple  = TransformValue("SOP", _.getAs[ID]("SOP"))

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
  var reply: Option[RequestNReply] = None


  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      reply = Some(rnr)

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


    }
    // Vi får states från Operation control
    case r @ Response(ids, attr, service, _) if service == operationController => {
      // Till att börja med är dessa tomma, så vi säger att alla som kör blir färdiga

      println(s"we got a state change")

      val res = activeSteps.map(stepCompleted)

      // Kolla om hela SOPen är färdigt. Inte säker på att detta fungerar
      if (res.foldLeft(false)(_ || _)){
        reply.foreach(rnr => rnr.reply ! Response(List(), SPAttributes("status"->"done"), rnr.req.service, rnr.req.reqID))
        self ! PoisonPill
      }

      val sopMap: Map[SOP, List[SOP]] = Map() //parent -> child
      //Tanken är man skapar en map där föräldrar pekar på  sina barn (key -> value)
      //detta kommer sedan användas när man kör en SOP genom att man börjar med en förälder
      //och kollar vilken typ den har (parallell eller sequence), för att veta hur den ska köra
      //sina barn. När ett barn har kört klart kommer den då säga till sin förälder att den är klar
      //och när alla barn till en förälder körts klart går man vidare i kedjan till nästa förälder
      // TODO: kanske borde använda en treemap? måste nämligen vara noga med att börja med rätt parent
      def createASopMap(sop: SOP): Map[SOP, List[SOP]] ={
        val sopSeq = sop.sop
        val nmbrOfChildren = sopSeq.length-1
        if (sop.sop.isEmpty) {
          sopMap
        }else if (nmbrOfChildren == 0) {
          sopMap + (sop -> List())
        } else if (nmbrOfChildren > 0){
          val sopList = List()
          for(child <- sopSeq){
            child -> sopList
            createASopMap(child)
          }
          sopMap + (sop -> sopList)
        } else {
          println("something went wrong")
        }
        sopMap
      }


      def getClassOfSop(sop: SOP): String ={
        sop match {
          case s: Parallel => "parallel"
          //case s: Alternative => "alternative"
          //case s: Arbitrary => "arbitrary"
          case s: Sequence => "sequence"
          //case s: SometimeSequence => "sometimeSequence"
          //case s: Other => "other"
          //case s: Hierarchy => "hierarchy"
          case _ => "noMatch"
        }
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
    println(s"executing sop $sop")
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
  def stepCompleted(complSOP: SOP): Boolean = {
    println(s"step $complSOP is completed. Parent is ${parents.get(complSOP)}")
    parents.get(complSOP) match {
      case Some(p: Parallel) => {
        val alreadyDoneSteps = parallelRuns.get(p)
        if (alreadyDoneSteps.isEmpty)
          parallelRuns = parallelRuns + (p->List(complSOP))
        else if(p.sop.length > alreadyDoneSteps.size)
          complSOP -> alreadyDoneSteps
        else if(p.sop.length == alreadyDoneSteps.size)
          stepCompleted(p)
        else {
          println("something went wrong in stepCompleted")
        }
        // Om alla är färdiga -> stepCompleted(p)
        // annars vänta
        false
      }
      case Some(p: Sequence) => {
        val parentSeq = p.sop
        val nbrOfChildren = parentSeq.length
        val current = parentSeq.indexOf(complSOP)
        if (current < nbrOfChildren) {
          activeSteps.filterNot(element => element == complSOP)
          executeSOP(parentSeq(current + 1))
        }
        else {
          stepCompleted(p)
        }
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






