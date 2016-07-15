package sp.system

import akka.actor._


/**
 * The starting object
 * Created by Kristofer on 2014-06-06.
 */
object SPActorSystem  {

  // The actor system used by all parts of SP. Maybe we will allow remote actors in the future
  implicit val system = ActorSystem("SP")

  val eventHandler = system.actorOf(Props[Dummy], "eventHandler")
  val modelHandler = system.actorOf(Props[Dummy])
  val serviceHandler = system.actorOf(Props[Dummy])
  val runtimeHandler = system.actorOf(Props[Dummy])
  val userHandler = system.actorOf(Props[Dummy])

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

class Dummy extends Actor {

  def receive = {
    case in ⇒
      println("The dummy got a message")
  }
}