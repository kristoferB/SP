package sp.launch

import sp.domain.SPAttributes
import sp.services.{PropositionParserActor}
import sp.system.messages._

/**
 * Created by Kristofer on 2014-06-27.
 */
object SP extends App {
  import sp.system.SPActorSystem._

  // Register Runtimes here
  runtimeHandler ! RegisterRuntimeKind("SimulationRuntime",
  sp.runtimes.SimulationRuntime.props,
  SPAttributes(Map("info"-> "En liten runtime")))


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


  // launch REST API
  sp.server.LaunchGUI.launch

  //val m = modelHandler ! "hej"


}
