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
  runtimeHandler ! RegisterRuntimeKind("DanielRuntime",
  sp.runtimes.DanielRuntime.props,
  SPAttributes(Map("info"-> "En liten runtime")))


  // Register services here
  serviceHandler ! RegisterService("PropositionParser",
  system.actorOf(PropositionParserActor.props, "PropositionParser"))


  import sp.services.relations._
  serviceHandler ! RegisterService("Relations",
    system.actorOf(RelationService.props, "Relations"))

  import sp.services.sopmaker._
  serviceHandler ! RegisterService("SopMaker",
    system.actorOf(SOPMakerService.props(modelHandler), "SOPMaker"))




  // launch REST API
  sp.server.LaunchGUI.launch

}
