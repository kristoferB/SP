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



  // launch REST API
  sp.server.LaunchGUI.launch

}
