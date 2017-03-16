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
  sealed trait API_OpcUARuntime
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


// simple example opc ua client useage
class OPC extends Actor {
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  val cp = "|var|CODESYS Control for Raspberry Pi SL.Application.Process_IOs."

  val nodes = List("feeder_exec", "newCylinder_var", "pnp1_mode", "pnp1from_var", "pnp1to_var", "p1_mode", "p1Transport_var", "p1Process_var", "convFree_var",
    "convMove_var", "convAtOut_var", "pnp2_mode", "pnp2to3_var", "pnp2to4_var", "p3_mode", "p3Process_var", "p4_mode", "p4Process_var")
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

      val bodySP = for {m <- message; b <- m.getBodyAs[APISP.StatusRequest]} yield b

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


