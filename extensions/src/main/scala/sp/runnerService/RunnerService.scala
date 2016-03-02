package sp.runnerService

import akka.actor._

import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._
import org.json4s.JsonAST.JInt
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
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
  val transformation = transformToList(transformTuple.productIterator.toList)
  //def props(eventHandler: ActorRef) = Props(classOf[RunnerService], eventHandler)
  def props = ServiceLauncher.props(Props(classOf[RunnerService]))
}

class RunnerService extends Actor with ServiceSupport {
  val serviceID = ID.newID

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val sopID = transform(RunnerService.transformTuple)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      //lista av tuplar som vi gör om till map
      val idMap: Map[ID, IDAble] = ids.map(item => item.id -> item).toMap

      //sop:en finns i idMap, fås som IDAble, men vi gör en for comp så den fås som ID
      val sop = for {
        item <- idMap.get(sopID) if item.isInstanceOf[SOP]
      } yield {
        item.asInstanceOf[SOP]
      }

      //lista med id
      val list = sop.map(runASop)

      replyTo ! Response(List(), SPAttributes("result" -> "done"), rnr.req.service, rnr.req.reqID)
      self ! PoisonPill
    }
  }

  def runASop(sop:SOP): Future[String] = {
    sop match {
      case p: Parallel =>
        println(s"Nu är vi i parallel $p")
        val fSeq = p.sop.map(runASop)
        Future.sequence(fSeq).map { list =>
          "done" //kolla sen så att alla verkligen är done!
        }
      case s: Sequence =>
        println(s"Nu är vi i sequence $s")
        if (s.sop.isEmpty) Future("done")
        else {
          val f = runASop(s.sop.head)
          f.flatMap(str => str match {
            case "done" =>
              runASop(Sequence() ++ s.sop.tail)
          })
        }
      case h: Hierarchy =>
        println(s"Nu är vi i hierarki $h")
        val f = test(h.operation)
        f.map(x => x match {
          case "done" => "done" //return true
          case "error" => "nope"
        })
    }
  }

  val sopMap: Map[SOP, List[SOP]] = Map() //parent -> child
  //Tanken är man skapar en map där föräldrar pekar på  sina barn (key -> value)
  //detta kommer sedan användas när man kör en SOP genom att man börjar med en förälder
  //och kollar vilken typ den har (parallell eller sequence), för att veta hur den ska köra
  //sina barn. När ett barn har kört klart kommer den då säga till sin förälder att den är klar
  //och när alla barn till en förälder körts klart går man vidare i kedjan till nästa förälder
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

def test(id: ID) = Future("done")

/*
def getOperation (sop: SOP) : List[Operation] = {
  val opList: List[Operation] = Nil
  for(o: Operation <- sop){
  opList :+ o
  }
  return opList
}*/

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
  case "sequence"=>{
  //for(o<-opList){execute(o), och skickar med när o är klar så nästa i for-loopen kan köra}
  //skickar tillbaka när hela opList har körts igenom så att for(s<- sopList ovan vet och kan skicka nästa sop till något annat)
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




