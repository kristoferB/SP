package sp.labkit.operations

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._


import sp.labkit.operations.{APILabkitControl => api}
import sp.labkit.operations.{API_OperationRunner => opAPI}

class LabkitOperationService extends Actor with ActorLogging with OperationRunnerLogic {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("answers", self)
  mediator ! Subscribe("events", self)




  // testing
  val testingMakingOps = makeMeOps("hej")
  val mess = opAPI.Setup("testing", ID.newID, testingMakingOps._1.toSet, testingMakingOps._2, testingMakingOps._3)
  println("sending some ops: "+ mess)

  mediator ! Publish("services", LabKitComm.makeMess(SPHeader(from = api.attributes.service, to = opAPI.attributes.service), mess))



  def receive = {
    case x: String if sender() != self =>
      val mess = SPMessage.fromJson(x)
      println("Getting things to LabkitOpService")
      println(mess)

      matchRequests(mess)
      matchRunnerAPI(mess)
      matchServiceRequests(mess)


  }





  def matchRequests(mess: Try[SPMessage]) = {


  }


  def matchRunnerAPI(mess: Try[SPMessage]) = {
    LabKitComm.extractOPReply(mess).map {
      case (h, opAPI.StateEvent(r, state)) =>
      case (h, opAPI.Runners(runners)) =>

    }

  }

  def matchServiceRequests(mess: Try[SPMessage]) = {
    LabKitComm.extractServiceRequest(mess) map { case (h, b) =>
      val spHeader = h.copy(from = api.attributes.service)
      mediator ! Publish("spevents", LabKitComm.makeMess(spHeader, APISP.StatusResponse(statusResponse)))
    }
  }






  val statusResponse = SPAttributes(
    "service" -> api.attributes.service,
    "api" -> "to be added with macros later",
    "tags" -> List("operation", "runtime", "labkit"),
    "attributes" -> api.attributes,
    "instanceID" -> ID.newID
  )

  // Sends a status response when the actor is started so service handlers finds it
  override def preStart() = {
    val mess = SPMessage.makeJson(SPHeader(from = api.attributes.service, to = "serviceHandler"), statusResponse)
    mediator ! Publish("spevents", mess)
  }


}

object LabkitOperationService {
  def props = Props(classOf[LabkitOperationService])
}





/*
 * Using a trait to make the logic testable
 */
trait OperationRunnerLogic extends MyOperationModel {
  case class AProductRun(id: ID, name: String, ops: Set[Operation], currentState: Map[ID, SPValue])
  var myProducts: Map[ID, AProductRun] = Map()




}

import sp.domain.logic.{ActionParser, PropositionParser}
trait MyOperationModel  {

  val things: Map[String, IDAble] = List(
    Thing("v_inPos"),
    Thing("v_pos1"),
    Thing("R1"),
    Thing("R2")
  ).map(x => x.name -> x).toMap



  // Här skapar vi unika operationer för varje produkt baserat på någon typ av input, tex från frontend
  // Detta blir det initiala "receptet" för en produkt. Vilken ability som skall utföra en viss op
  // kan anting hårdkodas in i denna metod, eller så gör ni det efter. Här kan ni också ha olika
  // alternativ om olika resurser kan göra en viss ability
  // Obs Ni måste skicka in mer saker i denna metod
  def makeMeOps(cylName: String) = {
    val init = Operation("init")
    val move = Operation("move")
    val remove = Operation("remove")

    val vars = things.values.toList ++ List(init, move, remove) // need to add ops if they are used in conditions

    val ops = List(
      init.copy(conditions = List(
        prop(vars, s"${things("v_inPos").id} == empty", List(s"${things("v_inPos").id} := $cylName"), "pre") // using the ${things("v_inPos").id} just to find typos
      )),
      move.copy(conditions = List(
        prop(vars, s"${things("v_inPos").id} == $cylName && ${things("R1").id}", List(s"${things("R1").id} := false"), "pre"),
        prop(vars, "", List(s"${things("R1").id} := true", s"${things("v_inPos").id} := empty", s"${things("v_pos1").id} := $cylName"), "post")
      )),
      remove.copy(conditions = List(
        prop(vars, s"${move.id} == f && ${things("R2").id}", List(s"${things("R2").id} := false"), "pre"), // it also works to have a op state condition like this
        prop(vars, "", List(s"${things("R2").id} := true", s"${things("v_pos1").id} := empty"), "post")
      ))
    )

    val opAbilityMap = Map( // init is not using an ability (this is not tested if it works 100%)
      move.id -> LabKitAbilityModel.a1.id,
      remove.id -> LabKitAbilityModel.a2.id
    )

    val state: Map[ID, SPValue] = Map(
      things("v_inPos").id -> "empty",
      things("v_pos1").id -> "empty",
      things("R1").id -> true,
      things("R2").id -> true
    )


    (ops, opAbilityMap, state)
  }









  def prop(vars: List[IDAble], cond: String, actions: List[String] = List(), kind: String = "pre") = {
    def c(condition: String): Option[Proposition] = {
      PropositionParser(vars).parseStr(condition) match {
        case Right(p) => Some(p)
        case Left(err) => println(s"Parsing failed on condition: $condition: $err"); None
      }
    }

    def a(actions: List[String]): List[Action] = {
      actions.flatMap { action =>
        ActionParser(vars).parseStr(action) match {
          case Right(a) => Some(a)
          case Left(err) => println(s"Parsing failed on action: $action: $err"); None
        }
      }
    }

    val cRes = if (cond.isEmpty) AlwaysTrue else c(cond).get
    val aRes = a(actions)

    PropositionCondition(cRes, aRes, SPAttributes("kind" -> kind))
  }
}
