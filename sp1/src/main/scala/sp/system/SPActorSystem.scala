package sp.system

import akka.actor._


/**
 * The starting object
 * Created by Kristofer on 2014-06-06.
 */
object SPActorSystem  {

  // The actor system used by all parts of SP. Maybe we will allow remote actors in the future
  implicit val system = ActorSystem("SP")

//  val eventHandler = system.actorOf(Props[Dummy], "eventHandler")
//  val modelHandler = system.actorOf(Props[Dummy])
//  val serviceHandler = system.actorOf(Props[Dummy])
//  val runtimeHandler = system.actorOf(Props[Dummy])
//  val userHandler = system.actorOf(Props[Dummy])
//
//  // TODO: Send this to all handlers instead of during construction
//  val handlers = SPHandlers(modelHandler, serviceHandler, eventHandler)


  val settings = SPSettings(system)

}

// some extra actors to help migrating to cluster. To be removed at a later stage

class PubActor(topic: String) extends Actor {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{ Publish }
  val mediator = DistributedPubSub(context.system).mediator
  def receive = {
    case x => mediator.tell(Publish(topic, x), sender())
  }
}

object PubActor {
  def props(topic: String) =  Props(classOf[PubActor], topic)
}
