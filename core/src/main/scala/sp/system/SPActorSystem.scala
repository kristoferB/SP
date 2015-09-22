package sp.system

import akka.actor._


/**
 * The starting object
 * Created by Kristofer on 2014-06-06.
 */
object SPActorSystem {

  // The actor system used by all parts of SP. Maybe we will allow remote actors in the future
  implicit val system = ActorSystem("sequenceplanner")

  val eventHandler = system.actorOf(EventHandler.props, "eventHandler")
  val modelHandler = system.actorOf(ModelHandler.props, "modelHandler")
  val serviceHandler = system.actorOf(ServiceHandler.props(modelHandler, eventHandler), "serviceHandler")
  val runtimeHandler = system.actorOf(RuntimeHandler.props, "runtimeHandler")
  val userHandler = system.actorOf(sp.users.UserHandler.props, "userHandler")

  val handlers = SPHandlers(modelHandler, serviceHandler, eventHandler)


  val settings = SPSettings(system)

}

// TODO: Broadcast handlers to all handlers after creation
case class SPHandlers(
           modelHandler : ActorRef,
           serviceHandler : ActorRef,
           eventHandler : ActorRef
)

class a extends Actor {
  def receive = {
    case _ =>
  }
}
