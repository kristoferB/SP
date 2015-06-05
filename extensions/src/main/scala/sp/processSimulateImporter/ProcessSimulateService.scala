package sp.processSimulateImporter

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import org.json4s.native.Serialization._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.camel.{ CamelExtension, CamelMessage, Consumer, Producer }

// activemq part
class ProcessSimulateAMQ extends Actor with Producer {
  implicit val timeout = Timeout(1 seconds)
  def endpointUri = "activemq:PS"
}

class ProcessSimulateService(modelHandler: ActorRef, psAmq: ActorRef) extends Actor {
  implicit val timeout = Timeout(1 seconds)
  val ps_timeout = Timeout(5 seconds)

  def addObjectFromJSON(json : String, modelid : ID) = {
    val idable = read[IDAble](json)
    modelHandler ! UpdateIDs(modelid, 0, List(idable))
  }

  def receive = {
    case Request(_, attr) => {
      val reply = sender
      extract(attr) match {
        case Some(("createOp",model,name)) => {
          val json = """{ "command" : "create_compound_op", "params":{"name":"""" + name + """"}}"""
          val result = Await.result(psAmq ? json, ps_timeout.duration)
          result match {
            case CamelMessage(body,headers) => addObjectFromJSON(body.toString, model)
          }
          reply ! "all ok"
        }

        case Some(("import",model,param)) => {
          val json = """{ "command" : "get_all_tx_objects" } """
          val result = Await.result(psAmq ? json, ps_timeout.duration)
          result match {
            case CamelMessage(body,headers) => {
              val idables = read[List[IDAble]](body.toString)
              modelHandler ! UpdateIDs(model, 0, idables)
            }
          }
          reply ! "all ok"
        }

        case Some(("import_single",model,param)) => {
          val json = """{ "command" : "get_tx_object", "params":{"txid":""""+param+""""}}"""
          val result = Await.result(psAmq ? json, ps_timeout.duration)
          result match {
            case CamelMessage(body,headers) => addObjectFromJSON(body.toString, model)
          }
          reply ! "all ok"
        }
          

        case _ => reply ! SPError("Ill formed request");
      }
    }
  }

  def extract(attr: SPAttributes) = {
    for {
      command <- attr.getAs[String]("command")
      model <- attr.getAs[ID]("model")
      param <- attr.getAs[String]("param")
    } yield (command, model, param)
  }  
}

object ProcessSimulateService {
  def props(modelHandler: ActorRef, psAmq: ActorRef) = Props(classOf[ProcessSimulateService], modelHandler, psAmq)
}
