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

case class ModalaSetup(inputdata: String, rootHierarchy: ID)

object ModalaService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "DALAHÄSTAR"
    ),
    "setup" -> SPAttributes(
      "inputdata" -> KeyDefinition("String", List(), Some("")),
      "rootHierarchy"-> KeyDefinition("ID", List(), None)
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
      println(s"JAA: $msg")
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
      //val amqConsumer = context.actorOf(ModalaAMQConsumer.props(replyTo,self, r, progress)) // start listening for replies

      progress ! SPAttributes("progress" -> "making a MODALA request")

      val setup = transform(ModalaService.transformTuple)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      val test = sp.areus.dummyData.createSarmadJson

      generateReq(ids, setup.rootHierarchy)

      //amqProducer ! setup.inputdata
    }
    case DalaResponse(message,caller,Request(s,a,i,r),progress) => {
      import org.json4s._
      import org.json4s.native.JsonMethods._
      val json = parse(message)
      caller ! Response(List(), SPAttributes("final result" -> json), s, r)
      sender() ! PoisonPill // don't need consumer any more
      progress ! PoisonPill
      self ! PoisonPill
    }
    case _ => sender ! SPError("Ill formed request");
  }

  def generateReq(ids: List[IDAble], rootID: ID) = {
    val idMap = ids.map(x => x.id -> x).toMap

    val root = tryWithOption(idMap(rootID).asInstanceOf[HierarchyRoot]).getOrElse(HierarchyRoot("empty"))

    val res =  (for {
      ch <- root.children
      if idMap.get(ch.item).isInstanceOf[Some[Thing]]
      tch <- ch.children
      if idMap.get(tch.item).isInstanceOf[Some[Operation]]
      och <- tch.children
      child <- idMap.get(och.item)
    } yield (idMap(tch.item).asInstanceOf[Operation], child)).
      foldLeft(Map[Operation, List[IDAble]]()){(result, tuple) =>
      val op = tuple._1
      val item = tuple._2
      result + (op -> (item :: result.get(tuple._1).getOrElse(List[IDAble]())))
    }

    println(s"res got: ${res.map(t => t._1.name -> t._2.map(_.name))}")

  }

  def tryWithOption[T](t: => T): Option[T] = {
    try {
      Some(t)
    } catch {
      case e: Exception => None
    }
  }

}
