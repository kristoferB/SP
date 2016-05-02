package sp.launch

import sp.runtimes.opc.PLCRuntime
import sp.services.{ PropositionParserService }
import sp.system.ServiceExample
import sp.system.messages._
import sp.domain._

import scala.io.Source

/**
 * Created by Kristofer on 2014-06-27.
 */
object SP extends App {

  import sp.system.SPActorSystem._
  import sp.domain.Logic._

  // Merge runtimes to services instead
  //  runtimeHandler ! RegisterRuntimeKind("SimulationRuntime",
  //  sp.runtimes.SimulationRuntime.props,
  //  SPAttributes("info"-> "Simulate system behavior by executing operations"))
  //
  //  runtimeHandler ! RegisterRuntimeKind("PLCRuntime",
  //    PLCRuntime.props,
  //    SPAttributes("info"-> "Show status of and control a PLC"))

  // Register services here
  serviceHandler ! RegisterService("PropositionParser",
    system.actorOf(PropositionParserService.props, "PropositionParser"))

  println("registering relation service")
  import sp.services.relations._
  serviceHandler ! RegisterService("RelationIdentification",
    system.actorOf(RelationIdentification.props, "RelationIdentification"),
    RelationIdentification.specification,
    RelationIdentification.transformation)

  import sp.services.relations._
  serviceHandler ! RegisterService("Relations",
    system.actorOf(RelationService.props(modelHandler, serviceHandler, "ConditionsFromSpecsService"), "Relations"))

  import sp.services.sopmaker._
  serviceHandler ! RegisterService("SOPMaker",
    system.actorOf(SOPMakerService.props(modelHandler), "SOPMaker"))

  import sp.services.specificationconverters._
  serviceHandler ! RegisterService("ConditionsFromSpecsService",
    system.actorOf(ConditionsFromSpecsService.props(modelHandler), "ConditionsFromSpecsService"))

  serviceHandler ! RegisterService("Example",
    system.actorOf(ServiceExample.props, "Example"),
    ServiceExample.specification,
    ServiceExample.transformation)

  import sp.extensions.DummySopService._
  serviceHandler ! RegisterService("DummySop",
    system.actorOf(DummySopService.props, "DummySop"),
    DummySopService.specification,
    DummySopService.transformation)

  //  import sp.areus._
  //
  //  serviceHandler ! RegisterService("DelmiaV5Service",
  //    system.actorOf(DelmiaV5Service.props(modelHandler), "DelmiaV5Service"))
  //
  //  serviceHandler ! RegisterService("ImportKUKAFileService",
  //    system.actorOf(ImportKUKAFileService.props(modelHandler), "ImportKUKAFileService"))
  //
  //  serviceHandler ! RegisterService("VCImportService",
  //    system.actorOf(VCImportService.props(modelHandler), "VCImportService"))
  //
  import sp.jsonImporter._

  val jsonActor = system.actorOf(ImportJSONService.props(modelHandler), "ImportJSONService")
  serviceHandler ! RegisterService("ImportJSONService", jsonActor)
  //
  //  import sp.merger._
  //
  //  serviceHandler ! RegisterService("ProductAbilityMerger",
  //    system.actorOf(ProductAbilityMerger.props(modelHandler), "ProductAbilityMerger"))
  //
  import sp.virtcom._

  serviceHandler ! RegisterService("CreateOpsFromManualModel",
    system.actorOf(CreateOpsFromManualModelService.props, "CreateOpsFromManualModel"),
    CreateOpsFromManualModelService.specification,
    CreateOpsFromManualModelService.transformation)

  serviceHandler ! RegisterService("SynthesizeModelBasedOnAttributes",
    system.actorOf(SynthesizeModelBasedOnAttributesService.props(modelHandler), "SynthesizeModelBasedOnAttributes"),
    SynthesizeModelBasedOnAttributesService.specification)

  serviceHandler ! RegisterService("ExtendIDablesBasedOnAttributes",
    system.actorOf(ExtendIDablesBasedOnTheirAttributes.props, "ExtendIDablesBasedOnAttributes"),
    ExtendIDablesBasedOnTheirAttributes.specification)

  serviceHandler ! RegisterService("CreateInstanceModelFromTypeModel",
    system.actorOf(CreateInstanceModelFromTypeModelService.props, "CreateInstanceModelFromTypeModel"),
    CreateInstanceModelFromTypeModelService.specification,
    CreateInstanceModelFromTypeModelService.transformation)

  serviceHandler ! RegisterService("CreateParallelInstance",
    system.actorOf(CreateParallelInstanceService.props(serviceHandler), "CreateParallelInstance"),
    CreateParallelInstanceService.specification,
    CreateParallelInstanceService.transformation)

  import sp.areus._

  serviceHandler ! RegisterService("ImportLogFiles",
    system.actorOf(ImportLogFiles.props, "ImportLogFiles"),
    ImportLogFiles.specification,
    ImportLogFiles.transformation)

  serviceHandler ! RegisterService("createGantt",
    system.actorOf(CreateGanttChart.props, "createGantt"),
    CreateGanttChart.specification,
    CreateGanttChart.transformation)
  serviceHandler ! RegisterService("transformTrajectories",
    system.actorOf(TransformTrajectories.props, "transformTrajectories"),
    TransformTrajectories.specification,
    TransformTrajectories.transformation)
  serviceHandler ! RegisterService("MakeGanttTrajectory",
    system.actorOf(MakeNewGanttTrajectory.props, "MakeGanttTrajectory"),
    MakeNewGanttTrajectory.specification,
    MakeNewGanttTrajectory.transformation)

  import sp.opcrunner._

  serviceHandler ! RegisterService("OpcRunner",
    system.actorOf(OpcRunner.props, "OpcRunner"), OpcRunner.specification, OpcRunner.transformation)

  serviceHandler ! RegisterService("Simulation",
    system.actorOf(Simulation.props, "Simulation"), Simulation.specification, Simulation.transformation)

  // activemq + process simulate stuff
//  import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
//  import akka.camel.{ CamelExtension, CamelMessage, Consumer, Producer }
//  import org.apache.activemq.camel.component.ActiveMQComponent
//  import sp.processSimulateImporter._
//
//  val camel = CamelExtension(system)
//  val camelContext = camel.context
//  camelContext.addComponent("activemq", ActiveMQComponent.activeMQComponent(s"tcp://${settings.activeMQ}:61616"))
//  val psamq = system.actorOf(Props[ProcessSimulateAMQ], "ProcessSimulateAMQ")
//  serviceHandler ! RegisterService("ProcessSimulate",
//    system.actorOf(ProcessSimulateService.props(modelHandler, psamq), "ProcessSimulate"),
//    ProcessSimulateService.specification,
//    ProcessSimulateService.transformation)
//
//  import sp.areus.modalaService._
//  val modalaamqProducer = system.actorOf(Props[ModalaAMQProducer], "ModalaAMQProducer")
//  serviceHandler ! RegisterService("Modala",
//    system.actorOf(ModalaService.props(modalaamqProducer), "Modala"),
//    ModalaService.specification,
//    ModalaService.transformation)

  //  //Preload model from json-importer
  //    val file = Source.fromFile("./testFiles/gitIgnore/module1.json").getLines().mkString("\n")
  //    jsonActor ! Request("someString", SPAttributes("file" -> file, "name" -> "preloadedModel"))



  import sp.control._
  serviceHandler ! RegisterService(
    "OperationControl",
    system.actorOf(OperationControl.props(eventHandler), "OperationControl"),
    OperationControl.specification,
    OperationControl.transformation
  )

  import sp.exampleService._
  serviceHandler ! RegisterService(
    "ExampleService",
    system.actorOf(ExampleService.props, "ExampleService"),
    ExampleService.specification,
    ExampleService.transformation
  )

  import sp.calculator._
  serviceHandler ! RegisterService(
    "Calculator",
    system.actorOf(Calculator.props, "Calculator"),
    Calculator.specification,
    Calculator.transformation
  )

  import sp.optimizerService._
  serviceHandler ! RegisterService(
    "Operation Maker",
    system.actorOf(operationMaker.props, "operationMaker"),
    Calculator.specification,
    Calculator.transformation
  )

  import sp.psl._
  serviceHandler ! RegisterService(
    "PSLModel",
    system.actorOf(PSLModel.props, "PSLModel"),
    PSLModel.specification,
    PSLModel.transformation
  )

  import sp.runnerService._
  serviceHandler ! RegisterService(
    "RunnerService",
    system.actorOf(RunnerService.props(eventHandler, "OperationControl"), "RunnerService"),
    RunnerService.specification,
    RunnerService.transformation
  )


  serviceHandler ! RegisterService(
    "VariableOperationMapper",
    system.actorOf(VariableOperationMapper.props, "VariableOperationMapper"),
    VariableOperationMapper.specification,
    VariableOperationMapper.transformation
  )



  // launch REST API
  sp.server.LaunchGUI.launch
}
