package sp.launch


import sp.system._
import sp.system.messages._
import scala.concurrent.Await
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

import scala.io.Source

/**
 * Created by Kristofer on 2014-06-27.
 */
object SP extends App {
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
    system.actorOf(SynthesizeModelBasedOnAttributesService.props(modelHandler, serviceHandler),
      "SynthesizeModelBasedOnAttributes"),
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

  import sp.virtcom.BDDVerifier
  mediator ! Publish("serviceHandler", RegisterService("BDDVerifier",
    system.actorOf(BDDVerifier.props(modelHandler), "BDDVerifier"),
    BDDVerifier.specification, BDDVerifier.transformation))

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

  // import  sp.opcMilo._
  // mediator ! Publish("serviceHandler", RegisterService(
  //   "OpcUARuntime",
  //   system.actorOf(OpcUARuntime.props, "OpcUARuntime"),
  //   OpcUARuntime.specification,
  //   OpcUARuntime.transformation
  // ))

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

  import sp.labkit._

  // mediator ! Publish("serviceHandler", RegisterService(
  //   "OPC",
  //   system.actorOf(OPC.props(serviceHandler), "OPC"),
  //   OPC.specification,
  //   OPC.transformation
  // ))

  mediator ! Publish("serviceHandler", RegisterService(
    "OperationSummer",
    system.actorOf(OperationSummer.props, "OperationSummer"),
    OperationSummer.specification,
    OperationSummer.transformation
  ))

  mediator ! Publish("serviceHandler", RegisterService(
    "WidgetsBackend",
    system.actorOf(WidgetsBackend.props(eventHandler), "WidgetsBackend"),
    WidgetsBackend.specification,
    WidgetsBackend.transformation
  ))


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

  // launch REST API
  sp.server.LaunchGUI.launch
  scala.io.StdIn.readLine("Press ENTER to exit application.\n") match {
    case x =>
      system.terminate()
      // wait for actors to die
      Await.ready(system.whenTerminated, Duration(1, TimeUnit.MINUTES))
      // cleanup milo crap
      // MiloOPCUAClient.destroy()
  }

}
