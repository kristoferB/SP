package sp.labkit

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
  sealed trait
  API_OpcUARuntime
  // requests
  case class Connect(url: String) extends API_OpcUARuntime
  case class Disconnect() extends API_OpcUARuntime
  case class GetNodes() extends API_OpcUARuntime
  case class Subscribe(nodeIDs: List[String]) extends API_OpcUARuntime
  case class Write(node: String, value: SPValue) extends API_OpcUARuntime

  // answers
  case class ConnectionStatus(connected: Boolean) extends API_OpcUARuntime
  case class AvailableNodes(nodes: Map[String, String]) extends API_OpcUARuntime
  case class StateUpdate(state: Map[String, SPValue], timeStamp: String) extends API_OpcUARuntime

  object attributes {
    val service = "OpcUARuntime"
  }
}

object OPC {
  def props = Props(classOf[OPC])
}

import sp.labkit.{API_OpcUARuntime => api}

// simple example opc ua client useage
class OPC extends Actor {
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  val cp = "|var|CODESYS Control for Raspberry Pi SL.Application.Process_IOs."

  val nodes = List("feedRun", "feedFinish", "feedState", "feedError", "robot1Run", "robot1Target", "robot1State", "robot1ResetRun",
  "robot1ResetState", "robot2Run", "robot2Target", "robot2State", "robot2ResetRun", "robot2ResetState", "c1p1Run", "c1p1Dir",
  "c1p1State", "c1p1Sensor", "c1p2Run", "c1p2Dir", "c1p2State", "c1p2Sensor", "c1TimeRun", "c1TimeTime", "c1TimeState", "c2p1Run",
  "c2p1Dir", "c2p1State", "c2p1Sensor", "c3p1Run", "c3p1Dir", "c3p1State", "c3p1Sensor", "c3p2Run", "c3p2Dir", "c3p2State", "c3p2Sensor",
  "c3p3Run", "c3p3Dir", "c3p3State", "c3p3Sensor", "c3TimeRun", "c3TimeTime", "c3TimeState", "c4p1Run", "c4p1Dir", "c4p1State", "c4p1Sensor",
  "c4p2Run", "c4p2Dir", "c4p2State", "c4p2Sensor", "c4p3Run", "c4p3Dir", "c4p3State", "c4p3Sensor", "c4TimeRun", "c4TimeTime", "c4TimeState")
  val nodeIDsToNode = nodes.map{n=>((cp+n)->n)}.toMap

  val ops = nodes.filter(_.contains("mode"))
  var resourceState = ops.map(_ -> false).toMap

  var connected = false
  val url = "opc.tcp://192.168.0.50:4840"

  mediator ! Subscribe("answers", self)
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)

  self ! "connect"

  def receive = {
    case mess @ _ if {println(s"LABKIT MESSAGE: $mess from $sender"); false} => Unit

    case "connect" =>
      println("labkit: connecting to opc")
      val header = SPHeader(from = api.attributes.service,  to = api.attributes.service)
      val body = api.Connect(url)
      val message = SPMessage.makeJson(header, body)
      mediator ! Publish("services", message)


    case x: String =>
      val message = SPMessage.fromJson(x)
      val header = for {m <- message; h <- m.getHeaderAs[SPHeader]} yield h
      val bodyAPI = for {
        m <- message
        h <- header if h.to == api.attributes.service
        b <- m.getBodyAs[api.API_OpcUARuntime]
      } yield b

      val bodySP = for {m <- message; m.getBodyAs[APISP] if b == APISP.StatusRequest} yield b

      for {
        body <- bodyAPI
        h <- header
        oldMess <- message
      } yield {
        body match {
          case api.ConnectionStatus(connectionStatus) if !connected && connectionStatus =>
              connected = true
              val header = SPHeader(from = api.attributes.service,  to = api.attributes.service)
              val body = api.Subscribe(nodeIDsToNode.keys.toList)
              val message = SPMessage.makeJson(header, body)
              mediator ! Publish("services", message)


          case api.ConnectionStatus(connectionStatus) if !connected && connectionStatus =>
              context.system.scheduler.scheduleOnce(Duration(5, TimeUnit.SECONDS), self, "connect")

          case api.StateUpdate(state: Map[String, SPValue], timeStamp: String) =>
            val shortMap = state.map(p=>nodeIDsToNode(p._1)->p._2).toMap
            mediator ! Publish("raw", SPAttributes("state"->shortMap, "time" -> timeStamp).toJson)
          case _ =>
        }
      }

      for {
        body <- bodySP
        oldMess <- message
      } yield {
        val mess = oldMess.makeJson(SPHeader(from = api.attributes.service, to = "serviceHandler"), APISP.StatusResponse(statusResponse))
        mediator ! Publish("spevents", mess)

      }


  }

  val statusResponse = SPAttributes(
    "service" -> api.attributes.service,
    "api" -> "to be added with macros later",
    "groups" -> List("opc"),
    "attributes" -> api.attributes
  )

}


