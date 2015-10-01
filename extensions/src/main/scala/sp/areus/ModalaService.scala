package sp.areus.modalaService

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import akka.camel._

import scala.util._

case class ModalaSetup(inputdata: String)

object ModalaService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "DALAHÄSTAR"
    ),
    "setup" -> SPAttributes(
      "inputdata" -> KeyDefinition("String", List(), Some(""))
    )
  )

  val transformTuple  = (
    TransformValue("setup", _.getAs[ModalaSetup]("setup"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(amqProducer: ActorRef) = ServiceLauncher.props(Props(classOf[ModalaService], amqProducer))
}

// forward requests to Modala via active mq
class ModalaAMQProducer extends Actor with Producer {
  def endpointUri = "activemq:MODALA.QUERIES"
  override def oneway: Boolean = true
}

// listen to responses from modala via active mq
class ModalaAMQConsumer(caller : ActorRef, service : ActorRef, req : Request, progress : ActorRef) extends Actor with Consumer {
  implicit val timeout = Timeout(100 seconds)
  def endpointUri = "activemq:MODALA.RESPONSES"

  def receive = {
    case msg: CamelMessage => {
      msg.body match {
        case "progress" => {
          // relay progress messages
          progress ! SPAttributes("progress" -> "Modala has some new info...")
        }
        case _ => {
          // final result, relay back to main service...
          service ! DalaResponse(msg.body.toString, caller, req, progress)
        }
      }
    }
  }  
}

object ModalaAMQProducer {
  def props = Props(classOf[ModalaAMQProducer])
}

object ModalaAMQConsumer {
  def props(caller : ActorRef, service: ActorRef, req : Request, progress : ActorRef) = Props(classOf[ModalaAMQConsumer],caller,service,req, progress)
}

case class DalaResponse(message : String, caller : ActorRef, req : Request, progress : ActorRef)

class ModalaService(amqProducer: ActorRef) extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)
      val amqConsumer = context.actorOf(ModalaAMQConsumer.props(replyTo,self, r, progress)) // start listening for replies

      progress ! SPAttributes("progress" -> "making a MODALA request")

      val setup = transform(ModalaService.transformTuple)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      amqProducer ! setup.inputdata
    }
    case DalaResponse(message,caller,Request(s,a,i,r),progress) => {
      caller ! Response(List(), SPAttributes("final result" -> message), s, r)
      sender() ! PoisonPill // don't need consumer any more
      progress ! PoisonPill
      self ! PoisonPill
    }
    case _ => sender ! SPError("Ill formed request");
  }
}
