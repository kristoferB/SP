package sp.system

import akka.actor._
import akka.event.Logging
import org.json4s.JsonAST.{JObject, JNothing}
import sp.domain._
import sp.system.messages._
import sp.domain.Logic._

class ServiceHandler(mh: ActorRef, eh: ActorRef) extends Actor{
  val log = Logging(context.system, this)
  var actors: Map[String, ActorRef] = Map()
  var specs: Map[String, (SPAttributes, List[TransformValue[_]])] = Map()

  def receive = {
    case r @ RegisterService(service, ref, attr, transform) => {
      if (!actors.contains(service)) {
        actors += service -> ref
        val serviceSpec = attr + ServiceTalker.serviceHandlerAttributes
        specs = specs + (service -> (serviceSpec, transform))
        ref.tell(r, sender)
        eh ! ServiceInfo(service, serviceSpec)
      }
      else sender ! SPError(s"Service $service already registered")
    }
    case r @ RemoveService(s) => {
      if (actors.contains(s)) {
        actors(s).tell(r, sender())
        sender() ! r
        actors = actors - s
        specs = specs - s
        eh ! r
      }
      else sender ! SPError(s"Service ${s} does not exists")
      println(s"Service $s removed")
    }
    case r @ Request(s, _, _, _) => {
      if (actors.contains(s)){
        val spec = specs(s)
        println(s"in servicehandler: $r")
        ServiceTalker.validateRequest(r, spec._1, spec._2) match {
          case Right(req) => {
            println(s"in servicehandler everything ok: $req")

            val talker = context.actorOf(ServiceTalker.props(actors(s), mh, sender, spec._1, req, Some(eh)))
            talker.tell(req, sender())
          }
          case Left(e) => {
            sender() ! SPErrors(e)
            println(s"in servicehandler error: ${e}")

          }
        }
      }
      else sender() ! SPError(s"Service ${s} does not exists")
    }
    case m: ServiceCommand => {
      if (actors.contains(m.service)){
        actors(m.service).tell(m, sender)
      }
      else sender ! SPError(s"Service ${m.service} does not exists")
    }
    case GetServices => sender ! specs.map{case (name, attribute) => ServiceInfo(name,attribute._1)}.toList
  }
}

object ServiceHandler {
  def props(modelHandler: ActorRef, eventHandler: ActorRef) = Props(classOf[ServiceHandler], modelHandler, eventHandler)
}

