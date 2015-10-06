package sp.launch

import sp.runtimes.opc.PLCRuntime
import sp.services.{PropositionParserActor}
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
    system.actorOf(PropositionParserActor.props, "PropositionParser"))

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
    ServiceExample.transformation
  )

  import sp.extensions.DummySopService._
  serviceHandler ! RegisterService("DummySop",
    system.actorOf(DummySopService.props, "DummySop"),
    DummySopService.specification,
    DummySopService.transformation
  )


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
    CreateOpsFromManualModelService.transformation
  )

  serviceHandler ! RegisterService("SynthesizeModelBasedOnAttributes",
    system.actorOf(SynthesizeModelBasedOnAttributesService.props(modelHandler), "SynthesizeModelBasedOnAttributes"),
    SynthesizeModelBasedOnAttributesService.specification)

  serviceHandler ! RegisterService("ExtendIDablesBasedOnAttributes",
    system.actorOf(ExtendIDablesBasedOnTheirAttributes.props, "ExtendIDablesBasedOnAttributes"),
    ExtendIDablesBasedOnTheirAttributes.specification
  )

  serviceHandler ! RegisterService("CreateInstanceModelFromTypeModel",
    system.actorOf(CreateInstanceModelFromTypeModelService.props, "CreateInstanceModelFromTypeModel"),
    CreateInstanceModelFromTypeModelService.specification,
    CreateInstanceModelFromTypeModelService.transformation
  )

  import sp.areus._

  serviceHandler ! RegisterService("ImportLogFiles",
    system.actorOf(ImportLogFiles.props, "ImportLogFiles"),
    ImportLogFiles.specification,
    ImportLogFiles.transformation
  )

  serviceHandler ! RegisterService("OpSeqBasedOnTime",
    system.actorOf(SearchOpSeqTimeService.props, "OpSeqBasedOnTime"),
    SearchOpSeqTimeService.specification,
    SearchOpSeqTimeService.transformation
  )



  // activemq + process simulate stuff
  import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
  import akka.camel.{ CamelExtension, CamelMessage, Consumer, Producer }
  import org.apache.activemq.camel.component.ActiveMQComponent
  import sp.processSimulateImporter._

  val camel = CamelExtension(system)
  val camelContext = camel.context
  camelContext.addComponent("activemq", ActiveMQComponent.activeMQComponent(s"tcp://${settings.activeMQ}:61616"))
  val psamq = system.actorOf(Props[ProcessSimulateAMQ], "ProcessSimulateAMQ")
  serviceHandler ! RegisterService("ProcessSimulate",
    system.actorOf(ProcessSimulateService.props(modelHandler, psamq), "ProcessSimulate"),
    ProcessSimulateService.specification,
    ProcessSimulateService.transformation
  )

  import sp.areus.modalaService._
  val modalaamqProducer = system.actorOf(Props[ModalaAMQProducer], "ModalaAMQProducer")
  serviceHandler ! RegisterService("Modala",
    system.actorOf(ModalaService.props(modalaamqProducer), "Modala"),
    ModalaService.specification,
    ModalaService.transformation
  )

  //  //Preload model from json-importer
//    val file = Source.fromFile("./testFiles/gitIgnore/module1.json").getLines().mkString("\n")
//    jsonActor ! Request("someString", SPAttributes("file" -> file, "name" -> "preloadedModel"))

  // launch REST API
  sp.server.LaunchGUI.launch
}
