package sp.launch

import sp.services.{PropositionParserActor}
import sp.system.messages._

import scala.io.Source

/**
 * Created by Kristofer on 2014-06-27.
 */
object SP extends App {

  import sp.system.SPActorSystem._

  // Register Runtimes here
  runtimeHandler ! RegisterRuntimeKind("SimulationRuntime",
    sp.runtimes.SimulationRuntime.props,
    SPAttributes(Map("info" -> "En liten runtime")))


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

  import sp.areus._

  serviceHandler ! RegisterService("DelmiaV5Service",
    system.actorOf(DelmiaV5Service.props(modelHandler), "DelmiaV5Service"))

  serviceHandler ! RegisterService("ImportKUKAFileService",
    system.actorOf(ImportKUKAFileService.props(modelHandler), "ImportKUKAFileService"))

  serviceHandler ! RegisterService("VCImportService",
    system.actorOf(VCImportService.props(modelHandler), "VCImportService"))

  import sp.jsonImporter._

  val jsonActor = system.actorOf(ImportJSONService.props(modelHandler), "ImportJSONService")
  serviceHandler ! RegisterService("ImportJSONService", jsonActor)

  import sp.merger._

  serviceHandler ! RegisterService("ProductAbilityMerger",
    system.actorOf(ProductAbilityMerger.props(modelHandler), "ProductAbilityMerger"))

  import sp.virtcom._

  serviceHandler ! RegisterService("CreateManufOpsFromProdOpsService",
    system.actorOf(CreateManufOpsFromProdOpsService.props(modelHandler), "CreateManufOpsFromProdOpsService"))

  //  //Preload model from json-importer
//    val file = Source.fromFile("C:/Users/patrik/Box Sync/ModelsForROAR/pslFloorRoof_JSON.json").getLines().mkString("\n")
//    jsonActor ! Request("someString", SPAttributes(Map("file" -> file, "name" -> "preloadedModel")))

  // launch REST API
  sp.server.LaunchGUI.launch
}
