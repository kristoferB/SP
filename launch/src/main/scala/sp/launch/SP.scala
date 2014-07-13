package sp.launch

import sp.domain.SPAttributes
import sp.system.messages._

/**
 * Created by Kristofer on 2014-06-27.
 */
object SP extends App {
  import sp.system.SPActorSystem._

  // registrerar Daniel runtime
  runtimeHandler ! RegisterRuntimeKind("DanielRuntime",
                   sp.runtimes.DanielRuntime.props,
                   SPAttributes(Map("info"-> "En liten runtime")))


  // launch REST API
  sp.server.LaunchGUI.launch

}
