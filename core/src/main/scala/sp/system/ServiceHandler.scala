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
    case m: ServiceMessage => {
      if (actors.contains(m.service))
        actors(m.service).tell(m, sender)
      else sender ! SPError(s"Service ${m.service} does not exists")
    }
    case GetServices => sender ! actors.keys.toList
  }
}

object ServiceHandler {
  def props = Props(classOf[ServiceHandler])
}