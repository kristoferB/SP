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
    ),
    "command" -> SPAttributes(
      "commandType"->KeyDefinition("String", List("connect", "disconnect", "status", "subscribe", "unsubscribe", "execute", "raw"), Some("connect")),
      "execute" -> KeyDefinition("Option[ID]", List(), None)//,
      )
  )

  val transformTuple  = (
    TransformValue("SOP", _.getAs[ID]("SOP")),
    TransformValue("command", _.getAs[String]("command"))
    )
  val transformation = transformToList(transformTuple.productIterator.toList)
  //def props(eventHandler: ActorRef) = Props(classOf[RunnerService], eventHandler)
  def props = ServiceLauncher.props(Props(classOf[RunnerService]))
}

//borde inte låta RunnerService ha ModelMaking trait egentligen....
class RunnerService extends Actor with ServiceSupport with ModelMaking {
  val serviceID = ID.newID

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val sopID = transform(RunnerService.transformTuple._1)
      val command = transform(RunnerService.transformTuple._2)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      //plockar ut den ability som utgör SOP med id sopID
      //val abilitiesUsed = getAbilityToID(sopID)

      val sop = operationIDToSOP(sopID)
      val sopList: List[SOP] = Nil
      for (s: SOP <- sop) {
        sopList :+ s
      }

      for (s <- sopList) {
        execute(getOperation(s), getClassOfSop(s))
        //någonting som väntar på svar....

      }

      //plockar ut db,byte,bit - adress - som tillhör den/de abilities som ligger i abilitiesUsed
      //val address = getAddressToAbility(abilitiesUsed)

      //val myCommand = SPAttributes(
      //"writeToAddress" -> address,
      //"command" -> command
      //)
      //sendRaw(myCommand)

      //hur gör vi med detta?
      replyTo ! Response(List(), SPAttributes("result" -> "done"), rnr.req.service, rnr.req.reqID)
      self ! PoisonPill
    }
  }

  def getOperation (sop: SOP) : List[Operation] = {
    val opList: List[Operation] = Nil
    for(o: Operation <- sop){
    opList :+ o
    }
    return opList
  }

  def execute (opList: List[Operation], sopType: String)={
    sopType match{
      case "parallel" =>{
        //execute(opList) - ska köra alla operationer i opList samtidigt
        //nya execute skickar till operation control
        //skicka något tillbaka som säger till när den exekverat klart
      }
      case "alternative"=>{
        //skicka på något sätt
      }
      case "arbitrary"=>{
        //skicka på något sätt
      }
      case "sequence"=>{
        //for(o<-opList){execute(o), och skickar med när o är klar så nästa i for-loopen kan köra}
        //skickar tillbaka när hela opList har körts igenom så att for(s<- sopList ovan vet och kan skicka nästa sop till något annat)
      }
      case "sometimeSequence"=>{
        //skicka på något sätt
      }
      case "other"=>{
        //skicka på något sätt
      }
      case "hierarchy"=>{
        //skicka på något sätt
      }
      case "noMatch"=>{
        //skicka felmeddelande?
      }
    }
  }

  def getClassOfSop(sop: SOP): String ={
    sop match {
      case s: Parallel => "parallel"
      case s: Alternative => "alternative"
      case s: Arbitrary => "arbitrary"
      case s: Sequence => "sequence"
      case s: SometimeSequence => "sometimeSequence"
      case s: Other => "other"
      case s: Hierarchy => "hierarchy"
      case _ => "noMatch"
    }
  }
}




