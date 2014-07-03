package sp.system

import akka.actor._


/**
 * The starting object
 * Created by Kristofer on 2014-06-06.
 */
object SPActorSystem {

  // The actor system used by all parts of SP. Maybe we will allow remote actors in the future
  implicit val system = ActorSystem("sequenceplanner")

  val modelHandler = system.actorOf(ModelHandler.props, "modelHandler")
  val serviceHandler = system.actorOf(ServiceHandler.props, "serviceHandler")
  val runtimeHandler = system.actorOf(RunTimeHandler.props, "runtimeHandler")


  val settings = SPSettings(system)

}
