package sp.system

import akka.actor._


/**
 * The starting object
 * Created by Kristofer on 2014-06-06.
 */
object SPActorSystem  {

  // The actor system used by all parts of SP. Maybe we will allow remote actors in the future
  implicit val system = ActorSystem("SP")

  val eventHandler = system.actorOf(EventHandler.props, "eventHandler")
  val modelHandler = system.actorOf(ModelHandler.props, "modelHandler")
  val serviceHandler = system.actorOf(ServiceHandler.props(modelHandler, eventHandler), "serviceHandler")
  val runtimeHandler = system.actorOf(RuntimeHandler.props, "runtimeHandler")
  val userHandler = system.actorOf(sp.users.UserHandler.props, "userHandler")

  // TODO: Send this to all handlers instead of during construction
  val handlers = SPHandlers(modelHandler, serviceHandler, eventHandler)


  val settings = SPSettings(system)

}

//object RunMe extends App {
//  import SPActorSystem._
//
//  val s = system
//
//}



case class SPHandlers(
           modelHandler : ActorRef,
           serviceHandler : ActorRef,
           eventHandler : ActorRef
)

