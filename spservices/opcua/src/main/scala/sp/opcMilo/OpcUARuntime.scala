package sp.opcMilo

import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties
import org.joda.time.DateTime
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
import sp.messages._
import sp.messages.Pickles._
import scala.util.{Failure, Success, Try}


package API_OpcUARuntime {
  sealed trait API_OpcUARuntime
  sealed trait API_OpcUARuntimeSubTrait

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
import sp.opcMilo.{API_OpcUARuntime => api}

object OpcUARuntime {
  def props = Props(classOf[OpcUARuntime])
}

// simple example opc ua client useage
class OpcUARuntime extends Actor {
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)


  val client = new MiloOPCUAClient()
  var state = State(Map())

  def receive = {
    // case mess @ _ if {println(s"OPCUA MESSAGE: $mess from $sender"); false} => Unit

    case x: String =>
      // SPMessage uses the APIParser to parse the json string
      SPMessage.fromJson(x) match {
        case Success(mess) =>
          println(s"OPCUA got ${mess.toString}")

          val bodySP = for {b <- mess.getBodyAs[APISP.StatusRequest]} yield b
          bodySP.map { body =>
            mediator ! Publish("spevents", SPMessage.make(SPHeader(api.attributes.service, "serviceHandler"), statusResponse))
          }

          for {
            h <- mess.getHeaderAs[SPHeader]
            b <- mess.getBodyAs[api.API_OpcUARuntime] if h.to == api.attributes.service
          } yield {
            def mktup(l1: List[APISP], l2: List[api.API_OpcUARuntime]) = (l1,l2)
            val (spapis,apis) = b match {
              case api.Connect(url) =>
                client.connect(url)
                mktup(List(APISP.SPACK()), List(api.ConnectionStatus(client.isConnected)))
              case api.Disconnect() =>
                if(client.isConnected) client.disconnect
                mktup(List(APISP.SPACK()), List(api.ConnectionStatus(client.isConnected)))
              case api.GetNodes() =>
                val nodes = client.getAvailableNodes.map { case (i,dt) => (i,dt.toString) }.toMap
                mktup(List(APISP.SPACK()), List(api.ConnectionStatus(client.isConnected), api.AvailableNodes(nodes)))
              case api.Subscribe(nodes) =>
                if(client.isConnected) client.subscribeToNodes(nodes, self)
                mktup(List(APISP.SPACK()), List(api.ConnectionStatus(client.isConnected)))
              case api.Write(node, value) =>
                if(client.isConnected) client.write(node, value)
                mktup(List(APISP.SPACK()), List(api.ConnectionStatus(client.isConnected)))
              case x => mktup(List(APISP.SPError(s"Ill formed request: $x")), List())
            }

            val updHeader = h.copy(replyFrom = api.attributes.service, replyID = Some(ID.newID))
            spapis.foreach { m => SPMessage.make(updHeader, m).map { b => mediator ! Publish("answers", b.toJson) } }
            apis.foreach { m => SPMessage.make(updHeader, m).map { b => mediator ! Publish("answers", b.toJson) } }
          }
        case Failure(err) => {}
      }
    case StateUpdate(activeState) =>
      val header = SPHeader(from = api.attributes.service, to = "all")
      val body = api.StateUpdate(activeState, client.getCurrentTime.toString)
      SPMessage.make(header, body).map { m => mediator ! Publish("answers", m.toJson) }
    case _ => sender ! APISP.SPError("Ill formed request")
  }

  val statusResponse = SPAttributes(
    "service" -> API_OpcUARuntime.attributes.service,
    "api" -> "to be added with macros later",
    "groups" -> List("examples"),
    "allowRequests" -> true
  )

  override def postStop() = {
    if(client.isConnected) client.disconnect()
  }
}
