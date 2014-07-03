package sp.system

import akka.actor._
import akka.event.Logging
import sp.system.messages._

class ServiceHandler extends Actor{
  var actors: Map[String, ActorRef] = Map()
  val log = Logging(context.system, this)
  
  def receive = {
    case r @ RegisterService(service, ref) => {
      if (!actors.contains(service)) {
        actors += service -> ref
        ref.tell(r, sender)
      }
      else sender ! SPError(s"Service $service already registered")
    }
  }
}

object ServiceHandler {
  def props = Props(classOf[ServiceHandler])
}