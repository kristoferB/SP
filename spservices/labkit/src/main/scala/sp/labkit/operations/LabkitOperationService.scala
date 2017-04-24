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
  mediator ! Subscribe("eveents", self)


  import scala.concurrent.duration._
  import context.dispatcher
  context.system.scheduler.scheduleOnce(7 seconds, self, DelayStart)


  case object DelayStart


  def receive = {
    case DelayStart =>
      // testing
      val testingMakingOps = makeMeOps("hej")
      val mess = opAPI.Setup("testing", ID.newID, testingMakingOps._1.toSet, testingMakingOps._2, testingMakingOps._3)
      //println("sending some ops: "+ mess)

      mediator ! Publish("services", LabKitComm.makeMess(SPHeader(from = api.attributes.service, to = opAPI.attributes.service), mess))
    case x: String if sender() != self =>
      val mess = SPMessage.fromJson(x)

      matchRequests(mess)
      matchRunnerAPI(mess)
      matchServiceRequests(mess)


  }





  def matchRequests(mess: Try[SPMessage]) = {


  }


  def matchRunnerAPI(mess: Try[SPMessage]) = {
    LabKitComm.extractOPReply(mess).map {
      case (h, opAPI.StateEvent(r, state)) =>
        println("Runner: " + state)
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

    // Feeder
    Thing("feedSensor"),
    // Robot 1
    Thing("R1"),
    Thing("R2"),
    Thing("C1"),
    Thing("C2"),
    //Thing("C3"),
    //Thing("C4"),
    Thing("c2in"),
    Thing("robot1"),
    //Thing("robot2"),

    // Band 1
    Thing("c1p1Sensor"),
    Thing("c2p1Sensor")
  ).map(x => x.name -> x).toMap



  // Här skapar vi unika operationer för varje produkt baserat på någon typ av input, tex från frontend
  // Detta blir det initiala "receptet" för en produkt. Vilken ability som skall utföra en viss op
  // kan anting hårdkodas in i denna metod, eller så gör ni det efter. Här kan ni också ha olika
  // alternativ om olika resurser kan göra en viss ability
  // Obs Ni måste skicka in mer saker i denna metod
  def makeMeOps(cylName: String) = {
    val feeder = Operation("feeder")
    val robot1toFeedCylPick = Operation("robot1toFeedCylPick")
    val robot1to1put = Operation("robot1to1put")
    val robot1to2put = Operation("robot1to2put")
    val conv2proc1 = Operation("conv2proc1")
    val robot2to2pick = Operation("robot2to2pick")

    val vars = things.values.toList ++ List(feeder, robot1toFeedCylPick, robot1to1put, robot1to2put, conv2proc1) // need to add ops

    val ops = List(
      /*
      init.copy(conditions = List(
        prop(vars, s"${things("v_inPos").id} == empty", List(s"${things("v_inPos").id} := $cylName"), "pre") // using the ${things("v_inPos").id} just to find typos
      )),
      move.copy(conditions = List(
        prop(vars, s"${things("v_inPos").id} == $cylName && ${things("RR1").id}", List(s"${things("RR1").id} := false"), "pre"),
        prop(vars, "", List(s"${things("RR1").id} := true", s"${things("v_inPos").id} := empty", s"${things("v_pos1").id} := $cylName"), "post")
      )),
      remove.copy(conditions = List(
        prop(vars, s"${move.id} == f && ${things("RR2").id}", List(s"${things("RR2").id} := false"), "pre"), // it also works to have a op state condition like this
        prop(vars, "", List(s"${things("RR2").id} := true", s"${things("v_pos1").id} := empty"), "post")
      )),*/
      feeder.copy(conditions = List(
        prop(vars, s"${things("feedSensor").id} == empty", List(), "pre"),
        prop(vars, "", List(s"${things("feedSensor").id} := $cylName"), "post")
      )),
      robot1toFeedCylPick.copy(conditions = List(
        prop(vars, s"${things("feedSensor").id} == $cylName && ${things("C1").id} == available && ${things("R1").id} == available",
          List(s"${things("R1").id} := unavailable"), "pre"),
        prop(vars, "", List(s"${things("feedSensor").id} := empty", s"${things("robot1").id} := $cylName"), "post")
      )),
      robot1to1put.copy(conditions = List(
        prop(vars, s"${robot1toFeedCylPick.id} == fjhj", List(s"${things("C1").id} := unavailable"), "pre"),
        prop(vars, "", List(s"${things("robot1").id} := empty", s"${things("c1p1Sensor").id} := $cylName",
          s"${things("R1").id} := available"), "post")
      )),
      robot1to2put.copy(conditions = List(
        prop(vars, s"${things("c2in").id} == empty && ${things("robot1").id} == $cylName", List(s"${things("C2").id} := unavailable"), "pre"),
        prop(vars, "", List(s"${things("robot1").id} := empty", s"${things("c2in").id} := $cylName",
          s"${things("R1").id} := available"), "post")
      )),
      conv2proc1.copy(conditions = List(
        prop(vars, s"${things("c2p1Sensor").id} == empty && ${things("c2in").id} == $cylName", List(), "pre"),
        prop(vars, "", List(s"${things("c2p1Sensor").id} := $cylName && ${things("c2in").id} := empty"), "post")
      ))/*,
      robot2to2pick.copy(conditions = List(
        prop(vars, s"${conv2proc1.id} == f",
          List(s"${things("R2").id} := unavailable"), "pre"),
        prop(vars, "", List(s"${things("c2p1Sensor").id} := empty", s"${things("robot2").id} := $cylName",
          s"${things("C2").id} := available"), "post")//${things("c2p1Sensor").id} == $cylName && ${things("C4").id} == available && ${things("R2").id} == available
      ))*/

    )
//things("robot1").id} == $cylName things("c1p1Sensor").id} == empty && ${things("robot1").id} == $cylName
    val opAbilityMap = Map( // init is not using an ability (this is not tested if it works 100%)
      //move.id -> LabKitAbilityModel.feeder.id,
      //init.id -> LabKitAbilityModel.a1.id,
     // remove.id -> LabKitAbilityModel.a2.id
      feeder.id -> LabKitAbilityModel.feeder.id,
      robot1toFeedCylPick.id -> LabKitAbilityModel.robot1toFeedCylPick.id,
      robot1to1put.id -> LabKitAbilityModel.robot1to1put.id,
      robot1to2put.id -> LabKitAbilityModel.robot1to2put.id,
      conv2proc1.id -> LabKitAbilityModel.conv2proc1.id
      //robot2to2pick.id -> LabKitAbilityModel.robot2to2pick.id
    )

    val state: Map[ID, SPValue] = Map(
      things("feedSensor").id -> "empty",
      things("R1").id -> "available",
      things("R2").id -> "available",
      things("C1").id -> "available",
      things("C2").id -> "available",
      //things("C3").id -> "available",
      //things("C4").id -> "available",
      things("robot1").id -> "empty",
      //things("robot2").id -> "empty",
      things("c2in").id -> "empty",
      things("c1p1Sensor").id -> "empty",
      things("c2p1Sensor").id -> "empty"
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

