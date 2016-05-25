package sp.virtcom

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
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._

import scala.util._

object ProcessSimulate extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Pull/push data from/to Process Simulate"
    ),
    "setup" -> SPAttributes(
      "ip" -> KeyDefinition("String", List(), Some("0.0.0.0")),
      "topic" -> KeyDefinition("String", List(), Some("ps"))
    ),    
    "command" -> SPAttributes(
      "type" -> KeyDefinition("String", List("connect","disconnect",
        "export seq", "import basic ops", "import all", "import single", "update sim ids"), Some("export seq")),
      "sops" -> KeyDefinition("List[ID]", List(), Some(SPValue(List()))),
      "txid" -> KeyDefinition("String", List(), Some(""))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[BusSetup]("setup")),
    TransformValue("command", _.getAs[SPAttributes]("command"))
    )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(eventHandler: ActorRef) = Props(classOf[ProcessSimulate], eventHandler)
}

case class BusSetup(ip: String, topic: String)

class ProcessSimulate(eventHandler: ActorRef) extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100.seconds)
  val serviceID = ID.newID
  var bus: Option[ActorRef] = None
  var setup: Option[BusSetup] = None
  var serviceName: Option[String] = None

  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val setup = transform(ProcessSimulate.transformTuple._1)
      val command = transform(ProcessSimulate.transformTuple._2)
      
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      command.getAs[String]("type").get match {
        case "connect" => connect(setup)
        case "disconnect" => disconnect
        case "export seq" => exportSeq(ids, command.getAs[List[ID]]("sops").getOrElse(List()))
        case _ =>
      }

      replyTo ! Response(List(), connectedAttribute, service, serviceID)
    }
    case ConnectionEstablished(request, c) => {
      println("connected:"+request)
      setup.foreach{ s=>
        c ! ConsumeFromTopic(s.topic)
        bus = Some(c)
        eventHandler ! Progress(connectedAttribute, serviceName.get, serviceID)
      }
    }
    case ConnectionFailed(request, reason) => {
      println("failed:"+reason)
    }
      
    case _ => sender ! SPError("Ill formed request");
  }

  def connect(s: BusSetup)(implicit rnr: RequestNReply) = {
    setup = Some(s)
    serviceName = Some(rnr.req.service)
    println(s"ProcessSimulate: connecting to bus: $setup")
    ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${s.ip}:61616")
  }

  def connectedAttribute = {
    if (setup.isEmpty)
      SPAttributes("bus"->"Not connected")
    else if (bus.isEmpty)
      SPAttributes("bus"->"Connecting")
    else
      SPAttributes("bus"->"Connected")
  }  

  def disconnect = {
    println("ProcessSimulate: disconnecting from the bus.")
    bus.foreach(_ ! CloseConnection)
    setup = None
    bus = None    
  }

  override def postStop() = {
    disconnect
  }
  
  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }

  def createExportJsonHelper(sop : SOP, ids: List[IDAble], ack : SPAttributes=SPAttributes()) : SPAttributes = {
    import org.json4s.JString
    sop match {
      case h: Hierarchy =>
        val ops = ids.find(o => o.id == h.operation)
        ack merge (ops match {
          case Some(o) => SPAttributes("name" -> o.name, "simop" -> (o.attributes.getAs[String]("simop") match {
            case Some(txid) => txid
            case _ =>
              o.attributes.getAs[String]("txid") match {
                case Some(txid) => txid
                case _ => "dummy"
              }
          }), "needsToBeCompleted" -> h.conditions.flatMap({
            case PropositionCondition(AND(x),_,_) =>
              x.flatMap({
                case EQ(SVIDEval(otherOpId), ValueHolder(JString("f"))) =>
                  // for now, only handle when the operation is finished
                  ids.find(o => o.id == otherOpId).map(_.name)
                case _ => None
              })
            case _ => None
          }))
          case None => SPAttributes("name" -> "op does not exist")
        })
      case p: Parallel => {
        ack merge SPAttributes("parallel" -> p.sop.map(createExportJsonHelper(_, ids, ack)))
      }
      case s: Sequence => {
        ack merge SPAttributes("sequence" -> s.sop.map(createExportJsonHelper(_, ids, ack)))
      }
    }
  }

  def exportSeq(ids: List[IDAble], sops: List[ID])(implicit rnr: RequestNReply) = {
    val sopspecs = ids.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec]).filter(sop => sops.contains(sop.id))

    val json = SPAttributes("command" -> "create_op_chains",
      "params" -> SPAttributes("op_chains" -> sopspecs.map { spec =>
        val sop = spec.sop.map(createExportJsonHelper(_, ids))
        SPAttributes("name" -> spec.name, "sop" -> sop)
      })).pretty
    

    eventHandler ! Progress(SPAttributes("progress" -> "Request sent to PS. Waiting for answer"),
      serviceName.get, serviceID)
    bus.map {_ ? json}.foreach(handlePSAnswer(_).foreach(l=>{
      eventHandler ! Response(l, SPAttributes("info" -> "create_op_chain"), serviceName.get, serviceID)
    }))
  }

  def handlePSAnswer(f: Future[Any])(implicit rnr: RequestNReply): Future[List[IDAble]] = {
    // finds error by exception, since then the future fails
    f.map { answer =>
      val json = answer.asInstanceOf[AMQMessage].body.toString
      val psres = SPAttributes.fromJson(json).get
      val responseType = psres.getAs[String]("response_type").get

      responseType match {
        case "simple_ok" => List()
        case "list_of_items" => psres.getAs[List[IDAble]]("items").getOrElse(List())
        case "item" => List(psres.getAs[IDAble]("item")).flatMap(x=>x)
        case "error" => {
          val error_message = psres.getAs[String]("error").get
          throw new Exception(s"PS Error: $error_message")
        }
        case x@_ => {
          throw new Exception(s"Unrecognized response $x")
        }
      }
    }
  }

}
