package sp.processSimulateImporter

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

/**
 * Some unclear interfaces. What is items?
 */

case class PSsetup(command: String, sops: List[ID])

object ProcessSimulateService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Pull/push data from/to Process Simulate"
    ),
    "setup" -> SPAttributes(
      "command" -> KeyDefinition("String", List("createOp", "import", "import_single"), Some("createOp")),
      "sops" -> KeyDefinition("List[ID]", List(), Some(SPValue(List())))
    )
  )

  val transformTuple  = (
    TransformValue("setup", _.getAs[PSsetup]("setup"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(modelHandler: ActorRef, psAmq: ActorRef) = ServiceLauncher.props(Props(classOf[ProcessSimulateService], modelHandler, psAmq))
}

// activemq part    Beövs det inte en consumer också?
class ProcessSimulateAMQ extends Actor with Producer {
  implicit val timeout = Timeout(100 seconds)
  def endpointUri = "activemq:PS"
}

object ProcessSimulateAMQ {
  def props = Props(classOf[ProcessSimulateAMQ])
}


class ProcessSimulateService(modelHandler: ActorRef, psAmq: ActorRef) extends Actor  with ServiceSupport {
  implicit val timeout = Timeout(100 seconds)
  import context.dispatcher

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)

      progress ! SPAttributes("progress" -> "creating a json for Process simulate")

      val setup = transform(ProcessSimulateService.transformTuple)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get
      
      val res = setup.command match {
        case "createOp" => createOp(core.model, ids, setup.sops, progress)
        case "import" => fetch(core.model, ids, progress)
        case "import_single" => fetch_single(core.model, ids, progress)
        case _ => throw new Exception("No such command! How to do this the scala way?")
      }

      res onComplete {
        case Success(resp) => {
          replyTo ! resp
          terminate(progress)
        }
        case Failure(t) => {
          replyTo ! SPError("Failed when communicating")
          terminate(progress)
        }
      }
    }
    case _ => sender ! SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }

  def createOp(model: Option[ID], ids: List[IDAble], sops: List[ID], progress: ActorRef)(implicit rnr: RequestNReply) = {
    val sopspecs = ids.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec]).filter(sop => sops.contains(sop.id))

    val json = SPAttributes("command"->"create_op_chains",
      "params"->Map("op_chains" -> sopspecs.map(sop => {
        val ops = sop match {
          case SOPSpec(name, s, attributes, id) => {
            s.head.sop.flatMap(x=>x match {
              case h: Hierarchy => ids.find(o=>o.id == h.operation)
            }).map(_.asInstanceOf[Operation])
          }
        }
        SPAttributes("name" -> sop.name,
          "ops" -> ops.map(o=>
            SPAttributes("name" -> o.name,"simop" -> (o.attributes.getAs[String]("simop") match {
              case Some(txid) => txid
              case _ => (o.attributes.getAs[String]("txid") match {
                case Some(txid) => txid
                case _ => "dummy"
              })
            }))))
      }))).toJson

    val f = psAmq ? json
    progress ! SPAttributes("progress" -> "Message send to PS. Waiting for answer")

    val items = handlePSAnswer(f)
    items.map{list => Response(list, SPAttributes("command" -> "create_op_chain"), rnr.req.service, rnr.req.reqID)}
  }  

  def fetch(model: Option[ID], ids: List[IDAble], progress: ActorRef)(implicit rnr: RequestNReply) = {
    val json = SPAttributes("command" -> "get_all_tx_objects") toJson

    val f = psAmq ? json
    progress ! SPAttributes("progress" -> "Message send to PS. Waiting for answer")

    val items = handlePSAnswer(f)
    items.map{list => Response(list, SPAttributes("command" -> "get_all_tx_objects"), rnr.req.service, rnr.req.reqID)}
  }

  def fetch_single(model: Option[ID], ids: List[IDAble], progress: ActorRef)(implicit rnr: RequestNReply) = {
    val txid = "" // params.getAs[String]("txid").getOrElse("")

    val json = SPAttributes("command" -> "get_tx_object", "params" -> Map("txid" -> txid)) toJson
    val f = psAmq ? json
    progress ! SPAttributes("progress" -> "Message send to PS. Waiting for answer")

    val items = handlePSAnswer(f)
    items.map{list => Response(list, SPAttributes("command" -> "get_all_tx_objects"), rnr.req.service, rnr.req.reqID)}
  }

  def handlePSAnswer(f: Future[Any])(implicit rnr: RequestNReply): Future[List[IDAble]] = {
    val p = Promise[List[IDAble]]()

    // finds error by exception, since then the future fails
    val res: Future[List[IDAble]] = f.map{answer =>
      val json = answer.asInstanceOf[CamelMessage].body.toString
      val psres = SPAttributes.fromJson(json).get
      val responseType = psres.getAs[String]("response_type").get

      responseType match {
        case "simple_ok" => {
          List()
        }
        case "list_of_items" => {
          psres.getAs[List[IDAble]]("items").get
        }
        case "item" => {
          List(psres.getAs[IDAble]("item").get)
        }
        case "error" => {
          val error_message = psres.getAs[String]("error").get
          throw new Exception(s"PS Error: $error_message")
        }
        case _ => {
          throw new Exception("Unrecognized response")
        }
      }
    }

    res.map(p.success(_))
    res.onFailure{case x => {
      p.failure(x)
      rnr.reply ! SPError(s"Failed when communicating with Process simulate: $x")
    }}

    p.future
  }

}
