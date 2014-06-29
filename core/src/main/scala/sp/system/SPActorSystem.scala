package sp.system

import akka.actor._


/**
 * The starting object
 * Created by Kristofer on 2014-06-06.
 */
object SPActorSystem {

  // The actor system used by all parts of SP. Maybe we will allow remote actors in the future
  implicit val system = ActorSystem("sequenceplanner")

  // temp test
  val modelHandler = system.actorOf(ModelHandler.props, "modelHandler")

  val settings = SPSettings(system)

}
