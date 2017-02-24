package sp.festo

import akka.actor._
import sp.domain._
import sp.domain.Logic._

import scala.concurrent.Future
import akka.util._
import akka.pattern.ask

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put, Subscribe}
import java.util.concurrent.TimeUnit

import org.joda.time.DateTime
import sp.messages._
import Pickles._

import scala.util.{Failure, Success, Try}

// to be able to use opcua runtime api
package API_OpcUARuntime {
  sealed trait API_OpcUARuntime

  // requests
  case class Connect(url: String) extends API_OpcUARuntime
  case class Disconnect(url: String) extends API_OpcUARuntime
  case class GetNodes(url: String) extends API_OpcUARuntime
  case class Subscribe(url: String, nodeIDs: List[String]) extends API_OpcUARuntime
  case class Write(url: String, node: String, value: SPValue) extends API_OpcUARuntime

  // answers
  case class ConnectionStatus(url: String, connected: Boolean) extends API_OpcUARuntime
  case class AvailableNodes(url: String, nodes: Map[String, String]) extends API_OpcUARuntime
  case class StateUpdate(url: String, state: Map[String, SPValue], timeStamp: String) extends API_OpcUARuntime

  object attributes {
    val service = "OpcUARuntime"
  }
}
import sp.festo.{API_OpcUARuntime => api}

object OPC {
  def props = Props(classOf[OPC])
}


// simple example opc ua client useage
class OPC extends Actor {
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  val cp = "|var|CECC-LK.Application."

  val spmapping = List("spSt1RFID").map("SPMapping."+_)
  val transport = List("xBG20","xBG21","xBG22","xBG23","xBG24","xMB20").map("Transport."+_)
  val nodes = spmapping ++ transport
  val nodeIDsToNode = nodes.map{n=>((cp+n)->n)}.toMap

  val ops = nodes.filter(_.contains("mode"))

  var b6connected = false
  var b14connected = false
  val b6url = "opc.tcp://172.20.6.1:4840"
  val b14url = "opc.tcp://172.20.14.1:4840"
  val prefixMap = Map(b6url -> "b6.", b14url -> "b14.")

  mediator ! Subscribe("answers", self)
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)

  self ! "connect"

  def receive = {
    //    case mess @ _ if {println(s"FESTO MESSAGE: $mess from $sender"); false} => Unit

    case "connect" =>
      println("festo: connecting to opc")
      if(!b6connected) {
        val header = SPHeader(from = api.attributes.service,  to = api.attributes.service, replyID = Some(ID.newID.value))
        val body = api.Connect(b6url)
        val message = SPMessage.make(header, body).map(_.toJson)
        message.map(m => mediator ! Publish("services", m))
      }

      if(!b14connected) {
        val header = SPHeader(from = api.attributes.service,  to = api.attributes.service, replyID = Some(ID.newID.value))
        val body = api.Connect(b14url)
        val message = SPMessage.make(header, body).map(_.toJson)
        message.map(m => mediator ! Publish("services", m))
      }

    case x: String =>
      val message = SPMessage.fromJson(x)
      val header = for {m <- message; h <- m.getHeaderAs[SPHeader]} yield h
      val bodyAPI = for {
        m <- message
        h <- header
        b <- m.getBodyAs[api.API_OpcUARuntime]
      } yield b

      val bodySP = for {m <- message; b <- m.getBodyAs[APISP.StatusRequest]} yield b

      for {
        body <- bodyAPI
        h <- header
        oldMess <- message
      } yield {
        body match {
          case api.ConnectionStatus(url,connectionStatus) if connectionStatus =>
            println("festo: opc connected")
            if(url == b6url && !b6connected) {
              b6connected = true
              val header = SPHeader(from = api.attributes.service, to = api.attributes.service, replyID = Some(ID.newID.value))
              val body = api.Subscribe(b6url,nodeIDsToNode.map(_._1).toList)
              val message = SPMessage.makeJson(header, body).map(m => mediator ! Publish("services", m))
            }
            if(url == b14url && !b14connected) {
              b14connected = true
              val header = SPHeader(from = api.attributes.service,  to = api.attributes.service, replyID = Some(ID.newID.value))
              val body = api.Subscribe(b14url,nodeIDsToNode.map(_._1).toList)
              val message = SPMessage.makeJson(header, body).map(m => mediator ! Publish("services", m))
            }

          case api.ConnectionStatus(url,connectionStatus) if !connectionStatus =>
            println("festo: could not connect to + " + url + ", retrying")
            context.system.scheduler.scheduleOnce(Duration(5, TimeUnit.SECONDS), self, "connect")

          case api.StateUpdate(url, state: Map[String, SPValue], timeStamp: String) =>
            val shortMap = state.map(p=>(prefixMap(url)+nodeIDsToNode(p._1))->p._2).toMap
            println("festo: new state" + shortMap.toString)
            mediator ! Publish("raw", SPAttributes("state"->shortMap, "time" -> timeStamp).toJson)
          case x =>
        }
      }

      for {
        body <- bodySP
        oldMess <- message
      } yield {
        val mess = oldMess.makeJson(SPHeader(api.attributes.service, "serviceHandler"), APISP.StatusResponse(statusResponse))
        mess.map(m => mediator ! Publish("spevents", m))

      }


  }

  val statusResponse = SPAttributes(
    "service" -> api.attributes.service,
    "api" -> "to be added with macros later",
    "groups" -> List("opc"),
    "attributes" -> api.attributes
  )

}
