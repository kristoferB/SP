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
  val serviceHandler = system.actorOf(ServiceHandler.props(modelHandler), "serviceHandler")
  val runtimeHandler = system.actorOf(RunTimeHandler.props, "runtimeHandler")
  val userHandler = system.actorOf(sp.users.UserHandler.props, "userHandler")


  val settings = SPSettings(system)

}

class a extends Actor {
  def receive = {
    case _ =>
  }
}
