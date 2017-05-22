package sp.labkit.operations

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._


import sp.labkit.operations.{APILabkitControl => api}
import sp.labkit.operations.{APIAbilityHandler => abapi}
import sp.labkit.operations.{API_OperationRunner => opAPI}

import scala.collection.mutable.ListBuffer
package API_LOS{
  sealed trait API_LOS
  case class sendThings(things: List[String], things2: List[String]) extends API_LOS
  object attributes {
    val service = "LOS"
  }
}
import sp.labkit.operations.{API_LOS => los}

class LabkitOperationService extends Actor with ActorLogging with OperationRunnerLogic {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("answers", self)
  mediator ! Subscribe("events", self)


  import scala.concurrent.duration._
  import context.dispatcher
  context.system.scheduler.scheduleOnce(4 seconds, self, DelayStart)
  println("KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK-")

  case object DelayStart
  var doOnce = false

  val runnerID = ID.newID

  def receive = {
    case DelayStart =>
      // testing
      val test = Try{
        val testingMakingOps = makeMeOps("cyl")
        val mess = opAPI.Setup("testing", runnerID, Set(), Map(), testingMakingOps._3)
        println("sending some ops: "+ mess)
        mediator ! Publish("services", LabKitComm.makeMess(SPHeader(from = api.attributes.service, to = opAPI.attributes.service), mess))
      }

      //println("UUUUUUUUUUUUUUUUUUUUUUUUU")
      //println(test)



    case x: String if sender() != self =>
      val mess = SPMessage.fromJson(x)

      //println("Operation Service got: "+ mess)

      matchRequests(mess)
      matchRunnerAPI(mess)
      matchServiceRequests(mess)
      matchStartString(mess)


  }

  def matchStartString(mess: Try[SPMessage]) = {
    LabKitComm.extractStartRequest(mess) map {
      case (h, b) =>
        if (b.string == "Starta jävla skit"){
          val test = Try{
            val testingMakingOps = makeMeOps("cyl")
            val mess = opAPI.AddOperations(runnerID, testingMakingOps._1.toSet, testingMakingOps._2)
            println("STARTA FFS "+ b.string)
            mediator ! Publish("services", LabKitComm.makeMess(SPHeader(from = api.attributes.service, to = opAPI.attributes.service), mess))
          }
        }
    }
  }
  /*
  def matchVariables(mess: Try[SPMessage]) = {
    LabKitComm.extractRequest(mess).map {
      case(h, APIAbilityHandler.sendThings(values)) =>
        values.filter(x => x.name.contains("Sensor"))
        values.map(a => things.get(a.name).get.id -> a.id).toMap
        values
    }
  }*/

  // Sorting out sensors
  var fromPLC = new ListBuffer[Thing]()
  var fromModel = new ListBuffer[Thing]()
  var i = 0
  var j = 0
  println("Size allVars: " + LabKitAbilityModel.allVars.size)
  for (i <- 0 to LabKitAbilityModel.allVars.size) {
    println(i)
  }
  if(!doOnce) {
    doOnce = true
    for (i <- 0 to LabKitAbilityModel.allVars.size-1) {
      if (LabKitAbilityModel.allVars(i).name.contains("Sensor")) {
        fromPLC += LabKitAbilityModel.allVars(i)
      }
    }
    for (i <- 0 to allVarsHere.size-1) {
      if (allVarsHere(i).name.contains("Sensor")) {
        fromModel+= allVarsHere(i)
      }
    }
    fromPLC.toList
    fromModel.toList
  }



  var PLC = new ListBuffer[String]()
  var Model = new ListBuffer[String]()

  def matchRequests(mess: Try[SPMessage]) = {
    i = 0
    j = 0


    LabKitComm.extractAbilityResponse(mess).map {
      case (h, APIVirtualDevice.StateEvent(r, id, state, diff)) =>
        PLC = new ListBuffer[String]()
        //println("HÄMTA ABILITY GREJ +++++++++++++++++++++++++++++++++")
        for (i <- 0 to fromPLC.size-1) {
          PLC += fromPLC(i).name
          val s = state.get(fromPLC(i).id).toString
          if (s.contains("false")){
            PLC += "false"
          }
          else if (s.contains("true")){
            PLC += "true"
          }
          else {
            PLC += ""
          }

        }
      case _ =>
    }
    LabKitComm.extractOPReply(mess).map {
      case (h, opAPI.StateEvent(r, state)) =>
        Model = new ListBuffer[String]()
        //println("HÄMTA MODEL -----------------------------")
        for (j <- 0 to fromModel.size - 1) {
          Model += fromModel(j).name
          val s = state.get(fromModel(j).id).get.toString
          if (s.contains("empty")){
            Model += "empty"
          }
          else if (s.contains("cyl")){
            Model += "cyl"
          }
          else {
            Model += ""
          }
        }
      case _ =>
        //println("--------------------------------------")
    }
    /*println("+++++++++++++++++++++++++++++++++++++++++++++")
    print("PLC - state")
    PLC.foreach(print)
    println
    print("Model - state")
    Model.foreach(print)
    println
    println("+++++++++++++++++++++++++++++++++++++++++++++")*/
    val spHeader = SPHeader(from = los.attributes.service, to = "ErrorHandler")
    val message = los.sendThings(PLC.toList, Model.toList)
    mediator ! Publish("error", SPMessage.makeJson(spHeader, message))
  }


  def matchRunnerAPI(mess: Try[SPMessage]) = {
    LabKitComm.extractOPReply(mess).map {
      case (h, opAPI.StateEvent(r, state)) =>
        //println("Runner: " + state)
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
  //Potentiellt så ska denna listan stå där nere där den nu används men deta kändes smidigare för mitt ändamål att komma åt alla things //Joel
  var allVarsHere = List(

    // Feeder
    Thing("feedSensor"),
    // Robot 1
    Thing("R1"),
    Thing("C1"),
    Thing("robot1"),
    Thing("convDone"),

    // Band 1
    Thing("c1p1Sensor"),
    Thing("c1p2Sensor")
  )
  val things: Map[String, IDAble] = allVarsHere.map(x => x.name -> x).toMap



  // Här skapar vi unika operationer för varje produkt baserat på någon typ av input, tex från frontend
  // Detta blir det initiala "receptet" för en produkt. Vilken ability som skall utföra en viss op
  // kan anting hårdkodas in i denna metod, eller så gör ni det efter. Här kan ni också ha olika
  // alternativ om olika resurser kan göra en viss ability
  // Obs Ni måste skicka in mer saker i denna metod
  def makeMeOps(cylName: String) = {
    val feeder = Operation("feeder")
    val robot1toFeedCylPick = Operation("robot1toFeedCylPick")
    val robot1to1put = Operation("robot1to1put")
    val conv1proc2 = Operation("conv1proc2")
    val conv1proc1 = Operation("conv1proc1")
    val conv1TimeRun  = Operation("conv1TimeRun")

    val vars = things.values.toList ++ List(feeder, robot1toFeedCylPick, robot1to1put, conv1proc1, conv1proc2, conv1TimeRun) // need to add ops

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
        prop(vars, s"${things("robot1").id} == $cylName", List(s"${things("C1").id} := unavailable"), "pre"),
        prop(vars, "", List(s"${things("robot1").id} := empty", s"${things("c1p1Sensor").id} := $cylName",
          s"${things("R1").id} := available"), "post")
      )),
      conv1proc2.copy(conditions = List(
        prop(vars, s"${things("c1p1Sensor").id} == $cylName", List(), "pre"),
        prop(vars, "", List(s"${things("c1p1Sensor").id} := empty", s"${things("c1p2Sensor").id} := $cylName"), "post")
      )),
      conv1proc1.copy(conditions = List(
        prop(vars, s"${things("c1p2Sensor").id} == $cylName", List(), "pre"),
        prop(vars, "", List(s"${things("c1p1Sensor").id} := $cylName", s"${things("c1p2Sensor").id} := empty",
          s"${things("convDone").id} := true"), "post")
      )),
      conv1TimeRun.copy(conditions = List(
        prop(vars, s"${things("convDone").id} == true", List(), "pre"),
        prop(vars, "", List(s"${things("c1p1Sensor").id} := empty",
          s"${things("convDone").id} := false"), "post")
      ))
      /*robot1to2put.copy(conditions = List(
        prop(vars, s"${things("c2in").id} == empty && ${things("robot1").id} == $cylName", List(s"${things("C2").id} := unavailable"), "pre"),
        prop(vars, "", List(s"${things("robot1").id} := empty", s"${things("c2in").id} := $cylName",
          s"${things("R1").id} := available"), "post")
      )),
      conv2proc1.copy(conditions = List(
        prop(vars, s"${things("c2p1Sensor").id} == empty && ${things("c2in").id} == $cylName", List(), "pre"),
        prop(vars, "", List(s"${things("c2p1Sensor").id} := $cylName && ${things("c2in").id} := empty"), "post")
      )),
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
      conv1proc1.id -> LabKitAbilityModel.conv1proc1.id,
      conv1proc2.id -> LabKitAbilityModel.conv1proc2.id,
      conv1TimeRun.id -> LabKitAbilityModel.conv1TimeRun.id

    )

    val state: Map[ID, SPValue] = Map(
      things("feedSensor").id -> "empty",
      things("R1").id -> "available",
      things("C1").id -> "available",
      things("robot1").id -> "empty",
      things("c1p1Sensor").id -> "empty",
      things("c1p2Sensor").id -> "empty",
      things("convDone").id -> "false"
    )
    //state.foreach(println)
    //things.foreach(println)
    //println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")

    //println(things.get("R1"))

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

