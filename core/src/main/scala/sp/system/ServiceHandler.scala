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
//    case c: Calculate => forward(c)
//    case c: CalculateToFuture => forward(Calculate(c.algorithm, c.parameters, sender))
//    case RegisterActor(id, ref) => actors += (id -> ref)
//  }
//
//  private def forward(calc: Calculate) = {
//    log.debug("ServiceRouter received Serve for id: "+ calc.algorithm)
//    if (actors.contains(calc.algorithm))
//        actors(calc.algorithm) ! calc
//      else {
//    	  log.debug("ServiceRouter missing algorithm: "+ calc.algorithm)
//    	  sender ! ServiceErrorString("ServiceRouter missing algorithm: "+ calc.algorithm)
//      }
  }
}