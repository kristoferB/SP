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


  var clients: Map[String, MiloOPCUAClient] = Map()
  var states: Map[String, State] = Map() // State(Map())

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
                if(!clients.contains(url)) {
                  // new client
                  val c = new MiloOPCUAClient()
                  c.connect(url)
                  clients += (url -> c)
                }
                mktup(List(APISP.SPACK()), List(api.ConnectionStatus(url, clients(url).isConnected)))
              case api.Disconnect(url) =>
                if(clients.contains(url) && clients(url).isConnected) {
                  val c = clients(url)
                  c.disconnect
                  clients = clients - url
                  mktup(List(APISP.SPACK()), List(api.ConnectionStatus(url, c.isConnected)))
                } else {
                  mktup(List(APISP.SPACK()), List(api.ConnectionStatus(url, false)))
                }
              case api.GetNodes(url) =>
                if(clients.contains(url) && clients(url).isConnected) {
                  val c = clients(url)
                  val nodes = c.getAvailableNodes.map { case (i,dt) => (i,dt.toString) }.toMap
                  mktup(List(APISP.SPACK()), List(api.ConnectionStatus(url, c.isConnected),
                    api.AvailableNodes(url, nodes)))
                } else {
                  mktup(List(APISP.SPError(s"Not connected")), List())
                }
              case api.Subscribe(url,nodes) =>
                if(clients.contains(url) && clients(url).isConnected) {
                  val c = clients(url)
                  if(c.isConnected) c.subscribeToNodes(nodes, self)
                  mktup(List(APISP.SPACK()), List(api.ConnectionStatus(url,c.isConnected)))
                } else {
                  mktup(List(APISP.SPError(s"Not connected")), List())
                }
              case api.Write(url, node, value) =>
                if(clients.contains(url) && clients(url).isConnected) {
                  val c = clients(url)
                  c.write(node, value)
                  mktup(List(APISP.SPACK()), List(api.ConnectionStatus(url,c.isConnected)))
                } else {
                  mktup(List(APISP.SPError(s"Not connected")), List())
                }
              case x => mktup(List(APISP.SPError(s"Ill formed request: $x")), List())
            }

            val updHeader = h.copy(replyFrom = api.attributes.service, replyID = Some(ID.newID))
            spapis.foreach { m => SPMessage.make(updHeader, m).map { b => mediator ! Publish("answers", b.toJson) } }
            apis.foreach { m => SPMessage.make(updHeader, m).map { b => mediator ! Publish("answers", b.toJson) } }
          }
        case Failure(err) => {}
      }
    case StateUpdate(url, activeState) =>
      val header = SPHeader(from = api.attributes.service)
      val body = api.StateUpdate(url, activeState, if(clients.contains(url)) clients(url).getCurrentTime.toString else "")
      SPMessage.make(header, body).map { m => mediator ! Publish("answers", m.toJson) }
      println("got state change and sent it")
    case _ => sender ! APISP.SPError("Ill formed request")
  }

  val statusResponse = SPAttributes(
    "service" -> API_OpcUARuntime.attributes.service,
    "api" -> "to be added with macros later",
    "groups" -> List("examples"),
    "allowRequests" -> true
  )

  override def postStop() = {
    clients.map { case (url,client) =>
      if(client.isConnected) client.disconnect()
    }
  }
}
