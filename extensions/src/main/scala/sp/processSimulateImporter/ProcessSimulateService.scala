package sp.processSimulateImporter

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import org.json4s.native.Serialization._
import org.json4s.JsonAST._
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
  implicit val timeout = Timeout(5 seconds)

  def addObjectFromJSON(json : String, modelid : ID) = {
    val idable = read[IDAble](json)
    modelHandler ! UpdateIDs(modelid, 0, List(idable))

  }
  def receive = {
    case Request(_, attr) => {
      val reply = sender
      extract(attr) match {
        case Some(("createOp",model,params)) => {

          val checkedItems = params.getAs[JObject]("items") match {
            case Some(list) => list.findObjectsWithField(List(("checked", JBool(true)))).unzip._1.flatMap(ID.makeID)
            case _ => List()
          }

          val spids = Await.result(modelHandler ? GetSpecs(model, s => checkedItems.contains(s.id) && s.isInstanceOf[SOPSpec]),timeout.duration)
          spids match {
            case SPIDs(items) => {
              for(item <- items) {
                val ops = item match {
                  case SOPSpec(name, s, attributes, id) => {
                    val ids = s.head.sop.map(x=>x match {
                      case h: Hierarchy => h.operation
                    } )
                    val objects = Await.result(modelHandler ? GetIds(model, ids.toList),timeout.duration)
                    objects match {
                      case SPIDs(ops) => ops
                    }
                  }
                }

                val json = SPAttributes("command"->"create_op_chain",
                  "params"->Map("ops" -> ops.map(o=>
                    SPAttributes("name" -> o.name,"simop" -> (o.attributes.getAs[String]("simop") match {
                      case Some(txid) => txid
                      case _ => (o.attributes.getAs[String]("txid") match {
                        case Some(txid) => txid
                        case _ => "dummy"
                      })
                    }))), "parent" -> item.name)) toJson

                // TODO: progressbar while waiting for answer...
                psAmq ! json

                // val result = Await.result(psAmq ? json, timeout.duration)
                // val children = result match {
                //   case CamelMessage(body,headers) => read[List[IDAble]](body.toString)
                // }
              }
            }
          }


          reply ! "all ok"
        }

        case Some(("import",model,params)) => {
          val json = SPAttributes("command"->"get_all_tx_objects") toJson
          val result = Await.result(psAmq ? json, timeout.duration)
          result match {
            case CamelMessage(body,headers) => {
              val idables = read[List[IDAble]](body.toString)
              modelHandler ! UpdateIDs(model, 0, idables)
            }
          }
          reply ! "all ok"
        }

        case Some(("import_single",model,params)) => {
          val txid = params.getAs[String]("txid") match {
            case Some(s) => s
            case _ => ""
          }
          val json = SPAttributes("command"->"get_tx_object", "params"->Map("txid"->txid)) toJson
          val result = Await.result(psAmq ? json, timeout.duration)
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
      params <- attr.getAs[SPAttributes]("params")
    } yield (command, model, params)
  }  
}

object ProcessSimulateService {
  def props(modelHandler: ActorRef, psAmq: ActorRef) = Props(classOf[ProcessSimulateService], modelHandler, psAmq)
}
