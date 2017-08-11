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

import scala.util.{Failure, Success, Try}

// to be able to use opcua runtime api
object APIOpcUARuntime {
  sealed trait Request
  sealed trait Response
  val service = "OpcUARuntime"

  // requests
  case class Connect(url: String) extends Request
  case object Disconnect extends Request
  case object GetNodes extends Request
  case class Subscribe(nodeIDs: List[String]) extends Request
  case class Write(node: String, value: SPValue) extends Request

  // answers
  case class ConnectionStatus(connected: Boolean) extends Response
  case class AvailableNodes(nodes: Map[String, String]) extends Response
  case class StateUpdate(state: Map[String, SPValue], timeStamp: String) extends Response


  object Request {
    implicit lazy val fOPCUARuntimeRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fOPCUARuntimeResponse: JSFormat[Response] = deriveFormatISA[Response]
  }
}

object OpcUARuntimeInfo {
  case class OpcUARuntimeRequest(request: APIOpcUARuntime.Request)
  case class OpcUARuntimeResponse(response: APIOpcUARuntime.Response)

  val req: com.sksamuel.avro4s.SchemaFor[OpcUARuntimeRequest] = com.sksamuel.avro4s.SchemaFor[OpcUARuntimeRequest]
  val resp: com.sksamuel.avro4s.SchemaFor[OpcUARuntimeResponse] = com.sksamuel.avro4s.SchemaFor[OpcUARuntimeResponse]

  val apischema = makeMeASchema(
    req(),
    resp()
  )

  val attributes: APISP.StatusResponse = APISP.StatusResponse(
    service = APIOpcUARuntime.service,
    instanceID = Some(ID.newID),
    instanceName = "",
    tags = List("runtime", "opc", "driver"),
    api = apischema,
    version = 1,
    attributes = SPAttributes.empty
  )
}

object OPC {
  def props = Props(classOf[OPC])
}

import sp.labkit.{APIOpcUARuntime => api}

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
      val header = SPHeader(from = api.service,  to = api.service)
      val body = api.Connect(url)
      val message = SPMessage.makeJson(header, body)
      mediator ! Publish("services", message)


    case x: String =>
      val message = SPMessage.fromJson(x)
      val header = for {m <- message; h <- m.getHeaderAs[SPHeader]} yield h
      val bodyAPI = for {
        m <- message
        h <- header if h.to == api.service
        b <- m.getBodyAs[api.Request]
      } yield b

      val bodySP = for {m <- message; b <- m.getBodyAs[APISP] if b == APISP.StatusRequest} yield b

      for {
        body <- bodyAPI
        h <- header
        oldMess <- message
      } yield {
        body match {
          case api.ConnectionStatus(connectionStatus) if !connected && connectionStatus =>
              connected = true
              val header = SPHeader(from = api.service,  to = api.service)
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
        val mess = oldMess.makeJson(SPHeader(from = api.service, to = "serviceHandler"), APISP.StatusResponse(statusResponse))
        mediator ! Publish("spevents", mess)

      }


  }

  val statusResponse = OpcUARuntimeInfo.attributes

}


