package sp.launch


import sp.system._
import sp.system.messages._

import scala.io.Source

/**
 * Created by Kristofer on 2014-06-27.
 */
object SP extends App {

import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import oscar.cp._

class VolvoRobots(params: SPAttributes) extends CPModel {
  def test = {
    val names = List("RS1_B940WeldSeg1", "RS1_B940WeldSeg2", "RS1_B941WeldSeg3", "RS1_B940WeldSeg4", "RS2_B940WeldSeg1", "RS2_B941WeldSeg2", "RS2_B940WeldSeg3", "RS2_B940WeldSeg4", "RS2_B941WeldSeg5", "RS2_B940WeldSeg6", "RS2_B941WeldSeg8", "RS2_B941WeldSeg9", "RS3_B941WeldSeg1", "RS3_B940WeldSeg2", "RS3_B941WeldSeg3", "RS3_B940WeldSeg4", "RS4_B940WeldSeg1", "RS4_B940WeldSeg2", "RS4_B941WeldSeg3", "RS4_B940WeldSeg4", "RS4_B940WeldSeg5")
    val nameMap = names.zipWithIndex.toMap
    val numTasks = names.size
    val durations = Array(1600,1000,1600,400,600,200,800,200,200,1200,1000,600,4618,2534,1539,2895,800,800,1000,200,2000);
    val totalDuration = durations.sum

    val precedences = List(("RS1_B940WeldSeg1","RS1_B940WeldSeg2"),("RS1_B940WeldSeg2","RS1_B941WeldSeg3"),("RS1_B941WeldSeg3","RS1_B940WeldSeg4"),("RS2_B940WeldSeg1","RS2_B941WeldSeg2"),("RS2_B941WeldSeg2","RS2_B940WeldSeg3"),("RS2_B940WeldSeg3","RS2_B940WeldSeg4"),("RS2_B940WeldSeg4","RS2_B941WeldSeg5"),("RS2_B941WeldSeg5","RS2_B940WeldSeg6"),("RS2_B940WeldSeg6","RS2_B941WeldSeg8"),("RS2_B941WeldSeg8","RS2_B941WeldSeg9"),("RS3_B941WeldSeg1","RS3_B940WeldSeg2"),("RS3_B940WeldSeg2","RS3_B941WeldSeg3"),("RS3_B941WeldSeg3","RS3_B940WeldSeg4"),("RS4_B940WeldSeg1","RS4_B940WeldSeg2"),("RS4_B940WeldSeg2","RS4_B941WeldSeg3"),("RS4_B941WeldSeg3","RS4_B940WeldSeg4"),("RS4_B940WeldSeg4","RS4_B940WeldSeg5"))

    val mutexes = List(("RS4_B941WeldSeg3", "RS2_B941WeldSeg2"), ("RS4_B940WeldSeg4", "RS2_B940WeldSeg1"), ("RS4_B941WeldSeg3", "RS2_B940WeldSeg3"), ("RS4_B940WeldSeg2", "RS2_B941WeldSeg2"), ("RS4_B940WeldSeg2", "RS2_B940WeldSeg3"), ("RS4_B940WeldSeg2", "RS2_B940WeldSeg1"), ("RS4_B941WeldSeg3", "RS2_B940WeldSeg1"), ("RS4_B940WeldSeg4", "RS2_B941WeldSeg2"), ("RS4_B940WeldSeg4", "RS2_B940WeldSeg3"),("RS4_B940WeldSeg2", "RS1_B941WeldSeg3"), ("RS4_B941WeldSeg3", "RS1_B940WeldSeg4"), ("RS4_B940WeldSeg1", "RS1_B940WeldSeg4"), ("RS4_B941WeldSeg3", "RS1_B941WeldSeg3"), ("RS4_B940WeldSeg1", "RS1_B941WeldSeg3"), ("RS4_B940WeldSeg2", "RS1_B940WeldSeg4"), ("RS1_B941WeldSeg3", "RS4_B940WeldSeg4"), ("RS4_B940WeldSeg4", "RS1_B940WeldSeg4"),("RS3_B940WeldSeg4", "RS1_B940WeldSeg1"), ("RS3_B940WeldSeg2", "RS1_B940WeldSeg1"), ("RS3_B941WeldSeg3", "RS1_B940WeldSeg1"),("RS4_B940WeldSeg5", "RS2_B940WeldSeg4"), ("RS4_B940WeldSeg5", "RS2_B940WeldSeg1"), ("RS4_B940WeldSeg5", "RS2_B941WeldSeg2"), ("RS4_B940WeldSeg5", "RS2_B940WeldSeg3"),("RS2_B941WeldSeg8", "RS3_B941WeldSeg1"))

    val forceEndTimes = List(("RS1_B940WeldSeg2","RS1_B941WeldSeg3"),
      ("RS1_B941WeldSeg3","RS1_B940WeldSeg4"),("RS2_B940WeldSeg1","RS2_B941WeldSeg2"),
      ("RS2_B941WeldSeg2","RS2_B940WeldSeg3"),("RS2_B941WeldSeg5","RS2_B940WeldSeg6"),
      ("RS3_B941WeldSeg3","RS3_B940WeldSeg4"),("RS4_B940WeldSeg2","RS4_B941WeldSeg3"),
      ("RS4_B941WeldSeg3","RS4_B940WeldSeg4"))
 
    var startTimes = Array.fill(numTasks)(CPIntVar(0, totalDuration))
    var endTimes = Array.fill(numTasks)(CPIntVar(0, totalDuration))
    var makespan = CPIntVar(0 to totalDuration)

    forceEndTimes.foreach { case (t1,t2) => add(startTimes(nameMap(t1)) + durations(nameMap(t1)) == startTimes(nameMap(t2)) ) }
    precedences.foreach { case (t1,t2) => add(startTimes(nameMap(t1)) + durations(nameMap(t1)) <= startTimes(nameMap(t2)) ) }
    mutexes.foreach { case (t1,t2) =>
      val leq1 = startTimes(nameMap(t1)) + durations(nameMap(t1)) <== startTimes(nameMap(t2))
      val leq2 = startTimes(nameMap(t2)) + durations(nameMap(t2)) <== startTimes(nameMap(t1))
      add(leq1 || leq2)
    }

    names.foreach { n =>
      add(endTimes(nameMap(n)) == startTimes(nameMap(n)) + durations(nameMap(n)))

      // except for time 0, operations can only start when something finishes
      // must exist a better way to write this
      val c = CPIntVar(0, numTasks)
      add(countEq(c, endTimes, startTimes(nameMap(n))))
      add(startTimes(nameMap(n)) === 0 || (c >>= 0))
    }
    add(maximum(endTimes, makespan))

//    minimize(makespan)
    search(binaryFirstFail(startTimes ++ Array(makespan)))

    var sols = Map[Int, Int]()
    onSolution {
      sols += makespan.value -> (sols.get(makespan.value).getOrElse(0) + 1)
      println("Makespan: " + makespan.value)
      println("Start times: ")
      names.foreach { name =>
        println(name + ": " + startTimes(nameMap(name)).value + " - " +
          durations(nameMap(name)) + " --> " + endTimes(nameMap(name)).value)
      }
      sols.foreach { case (k,v) => println(k + ": " + v + " solutions") }
    }

    val stats = start() // (nSols = 1)
    println("stats " + stats)
  }
}
  

  import sp.system.SPActorSystem._

  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
  val mediator = DistributedPubSub(system).mediator

  val modelHandler = system.actorOf(PubActor.props("modelHandler"))
  val serviceHandler = system.actorOf(PubActor.props("serviceHandler"))
  val eventHandler = system.actorOf(PubActor.props("eventHandler"))

  system.actorOf(ModelHandler.props, "modelHandler")
  system.actorOf(ServiceHandler.props, "serviceHandler")
  system.actorOf(EventHandler.props, "eventHandler")


  // Register services here

  import sp.services.PropositionParserService
  mediator ! Publish("serviceHandler", RegisterService("PropositionParser",
    system.actorOf(PropositionParserService.props, "PropositionParser")))

  println("registering relation service")
  import sp.services.relations._
  mediator ! Publish("serviceHandler", RegisterService("RelationIdentification",
    system.actorOf(RelationIdentification.props, "RelationIdentification"),
    RelationIdentification.specification,
    RelationIdentification.transformation))

  import sp.services.relations._
  mediator ! Publish("serviceHandler", RegisterService("Relations",
    system.actorOf(RelationService.props(modelHandler, serviceHandler, "ConditionsFromSpecsService"), "Relations")))

  import sp.services.sopmaker._
  mediator ! Publish("serviceHandler", RegisterService("SOPMaker",
    system.actorOf(SOPMakerService.props(modelHandler), "SOPMaker")))

  import sp.services.specificationconverters._
  mediator ! Publish("serviceHandler", RegisterService("ConditionsFromSpecsService",
    system.actorOf(ConditionsFromSpecsService.props(modelHandler), "ConditionsFromSpecsService")))

  mediator ! Publish("serviceHandler", RegisterService("Example",
    system.actorOf(ServiceExample.props, "Example"),
    ServiceExample.specification,
    ServiceExample.transformation))

  import sp.extensions.DummySopService._
  mediator ! Publish("serviceHandler", RegisterService("DummySop",
    system.actorOf(DummySopService.props, "DummySop"),
    DummySopService.specification,
    DummySopService.transformation))

  //  import sp.areus._
  //
  //  mediator ! Publish("serviceHandler", RegisterService)("DelmiaV5Service",
  //    system.actorOf(DelmiaV5Service.props(modelHandler), "DelmiaV5Service"))
  //
  //  mediator ! Publish("serviceHandler", RegisterService)("ImportKUKAFileService",
  //    system.actorOf(ImportKUKAFileService.props(modelHandler), "ImportKUKAFileService"))
  //
  //  mediator ! Publish("serviceHandler", RegisterService)("VCImportService",
  //    system.actorOf(VCImportService.props(modelHandler), "VCImportService"))
  //
  import sp.jsonImporter._

  val jsonActor = system.actorOf(ImportJSONService.props(modelHandler), "ImportJSONService")
  mediator ! Publish("serviceHandler", RegisterService("ImportJSONService", jsonActor))
  //
  //  import sp.merger._
  //
  //  mediator ! Publish("serviceHandler", RegisterService)("ProductAbilityMerger",
  //    system.actorOf(ProductAbilityMerger.props(modelHandler), "ProductAbilityMerger"))
  //
  import sp.virtcom._

  mediator ! Publish("serviceHandler", RegisterService("CreateOpsFromManualModel",
    system.actorOf(CreateOpsFromManualModelService.props, "CreateOpsFromManualModel"),
    CreateOpsFromManualModelService.specification,
    CreateOpsFromManualModelService.transformation))

  mediator ! Publish("serviceHandler", RegisterService("SynthesizeModelBasedOnAttributes",
    system.actorOf(SynthesizeModelBasedOnAttributesService.props(modelHandler), "SynthesizeModelBasedOnAttributes"),
    SynthesizeModelBasedOnAttributesService.specification))

  mediator ! Publish("serviceHandler", RegisterService("ExtendIDablesBasedOnAttributes",
    system.actorOf(ExtendIDablesBasedOnTheirAttributes.props, "ExtendIDablesBasedOnAttributes"),
    ExtendIDablesBasedOnTheirAttributes.specification))

  mediator ! Publish("serviceHandler", RegisterService("CreateInstanceModelFromTypeModel",
    system.actorOf(CreateInstanceModelFromTypeModelService.props, "CreateInstanceModelFromTypeModel"),
    CreateInstanceModelFromTypeModelService.specification,
    CreateInstanceModelFromTypeModelService.transformation))

  mediator ! Publish("serviceHandler", RegisterService("CreateParallelInstance",
    system.actorOf(CreateParallelInstanceService.props(serviceHandler), "CreateParallelInstance"),
    CreateParallelInstanceService.specification,
    CreateParallelInstanceService.transformation))

  mediator ! Publish("serviceHandler", RegisterService("SimpleShortestPath",
    system.actorOf(SimpleShortestPath.props, "SimpleShortestPath"),
    SimpleShortestPath.specification, SimpleShortestPath.transformation))


  import sp.areus._

  mediator ! Publish("serviceHandler", RegisterService("ImportLogFiles",
    system.actorOf(ImportLogFiles.props, "ImportLogFiles"),
    ImportLogFiles.specification,
    ImportLogFiles.transformation))

  mediator ! Publish("serviceHandler", RegisterService("createGantt",
    system.actorOf(CreateGanttChart.props, "createGantt"),
    CreateGanttChart.specification,
    CreateGanttChart.transformation))
  mediator ! Publish("serviceHandler", RegisterService("transformTrajectories",
    system.actorOf(TransformTrajectories.props, "transformTrajectories"),
    TransformTrajectories.specification,
    TransformTrajectories.transformation))
  mediator ! Publish("serviceHandler", RegisterService("MakeGanttTrajectory",
    system.actorOf(MakeNewGanttTrajectory.props, "MakeGanttTrajectory"),
    MakeNewGanttTrajectory.specification,
    MakeNewGanttTrajectory.transformation))

  import sp.opcrunner._

  mediator ! Publish("serviceHandler", RegisterService("OpcRunner",
    system.actorOf(OpcRunner.props, "OpcRunner"), OpcRunner.specification, OpcRunner.transformation))

  mediator ! Publish("serviceHandler", RegisterService("Simulation",
    system.actorOf(Simulation.props, "Simulation"), Simulation.specification, Simulation.transformation))

  import sp.virtcom.ProcessSimulate
  mediator ! Publish("serviceHandler", RegisterService("ProcessSimulate",
    system.actorOf(ProcessSimulate.props(modelHandler,eventHandler), "ProcessSimulate"),
    ProcessSimulate.specification, ProcessSimulate.transformation))

  import sp.virtcom.VolvoRobotSchedule
  mediator ! Publish("serviceHandler", RegisterService("VolvoRobotSchedule",
    system.actorOf(VolvoRobotSchedule.props(serviceHandler), "VolvoRobotSchedule"),
    VolvoRobotSchedule.specification, VolvoRobotSchedule.transformation))

//
//  import sp.areus.modalaService._
//  val modalaamqProducer = system.actorOf(Props[ModalaAMQProducer], "ModalaAMQProducer")
//  mediator ! Publish("serviceHandler", RegisterService)("Modala",
//    system.actorOf(ModalaService.props(modalaamqProducer), "Modala"),
//    ModalaService.specification,
//    ModalaService.transformation)

  //  //Preload model from json-importer
  //    val file = Source.fromFile("./testFiles/gitIgnore/module1.json").getLines().mkString("\n")
  //    jsonActor ! Request("someString", SPAttributes("file" -> file, "name" -> "preloadedModel"))



  import sp.control._
  mediator ! Publish("serviceHandler", RegisterService(
    "OperationControl",
    system.actorOf(OperationControl.props(eventHandler), "OperationControl"),
    OperationControl.specification,
    OperationControl.transformation
  ))

  import sp.robotCycleAnalysis._
  mediator ! Publish("serviceHandler", RegisterService(
    "RobotCycleAnalysis",
    system.actorOf(RobotCycleAnalysis.props(eventHandler), "RobotCycleAnalysis"),
    RobotCycleAnalysis.specification,
    RobotCycleAnalysis.transformation
  ))

//  import sp.exampleService._
//  mediator ! Publish("serviceHandler", RegisterService)(
//    "ExampleService",
//    system.actorOf(ExampleService.props, "ExampleService"),
//    ExampleService.specification,
//    ExampleService.transformation
//  )

//
//  import sp.calculator._
//  mediator ! Publish("serviceHandler", RegisterService)(
//    "Calculator",
//    system.actorOf(Calculator.props, "Calculator"),
//    Calculator.specification,
//    Calculator.transformation
//  )


  import sp.psl._
  mediator ! Publish("serviceHandler", RegisterService(
    "PSLModel",
    system.actorOf(PSLModel.props, "PSLModel"),
    PSLModel.specification,
    PSLModel.transformation
  ))

  import sp.psl.runnerService._
  val rs = system.actorOf(RunnerService.props(eventHandler, serviceHandler, "OperationControl"), "RunnerService")
  mediator ! Publish("serviceHandler", RegisterService(
    "RunnerService",
    rs,
    RunnerService.specification,
    RunnerService.transformation
  ))

//  mediator ! Publish("serviceHandler", RegisterService)(
//    "AutoTest",
//    system.actorOf(AutoTest.props(eventHandler,rs), "AutoTest"),
//    AutoTest.specification,
//    AutoTest.transformation
//  )


  mediator ! Publish("serviceHandler", RegisterService(
    "VariableOperationMapper",
    system.actorOf(VariableOperationMapper.props, "VariableOperationMapper"),
    VariableOperationMapper.specification,
    VariableOperationMapper.transformation
  ))

  mediator ! Publish("serviceHandler", RegisterService(
    "operatorService",
    system.actorOf(OperatorService.props(serviceHandler), "operatorService"),
    OperatorService.specification,
    OperatorService.transformation
  ))

  mediator ! Publish("serviceHandler", RegisterService(
    "OrderHandler",
    system.actorOf(OrderHandler.props(serviceHandler, eventHandler), "OrderHandler"),
    OrderHandler.specification,
    OrderHandler.transformation
  ))

  mediator ! Publish("serviceHandler", RegisterService(
    "OperatorInstructions",
    system.actorOf(OperatorInstructions.props(eventHandler), "OperatorInstructions"),
    OperatorInstructions.specification,
    OperatorInstructions.transformation
  )  )

  println("CP TEST COMING UP")
  val vr = new VolvoRobots(SPAttributes())
  vr.test



  // launch REST API
  sp.server.LaunchGUI.launch
  scala.io.StdIn.readLine("Press ENTER to exit application.\n") match {
    case x => system.terminate()
  }

}
