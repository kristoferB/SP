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
import scala.util.{Failure, Success, Try}


sealed trait API_OpcUARuntime
object API_OpcUARuntime {
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

  val service = "OpcUARuntime"
}

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
    case mess @ _ if {println(s"OPCUA MESSAGE: $mess from $sender"); false} => Unit

    case x: String =>
      // SPMessage uses the APIParser to parse the json string
      SPMessage.fromJson(x) match {
        case Success(mess) =>
          println(s"OPCUA got ${mess.toString}")

          // forward the API to another method if it is my API
          // It returns the json AST (upickle) that will be used in SPMessage
          val res = getMyMessage(mess).map{
            case API_OpcUARuntime.Connect(url) =>
              client.connect(url)
              List(APIParser.writeJs(APISP.SPACK()), APIParser.writeJs(API_OpcUARuntime.ConnectionStatus(client.isConnected)))
            case API_OpcUARuntime.Disconnect() =>
              if(client.isConnected) client.disconnect
              List(APIParser.writeJs(APISP.SPACK()), APIParser.writeJs(API_OpcUARuntime.ConnectionStatus(client.isConnected)))
            case API_OpcUARuntime.GetNodes() =>
              val nodes = client.getAvailableNodes.map { case (i,dt) => (i,dt.toString) }.toMap
              List(APIParser.writeJs(APISP.SPACK()), APIParser.writeJs(API_OpcUARuntime.ConnectionStatus(client.isConnected)),
                APIParser.writeJs(API_OpcUARuntime.AvailableNodes(nodes)))
            case API_OpcUARuntime.Subscribe(nodes) =>
              if(client.isConnected) client.subscribeToNodes(nodes, self)
              List(APIParser.writeJs(APISP.SPACK()), APIParser.writeJs(API_OpcUARuntime.ConnectionStatus(client.isConnected)))
            case API_OpcUARuntime.Write(node, value) =>
              if(client.isConnected) client.write(node, value)
              List(APIParser.writeJs(APISP.SPACK()), APIParser.writeJs(API_OpcUARuntime.ConnectionStatus(client.isConnected)))
            case x => List(APIParser.writeJs(APISP.SPError(s"Ill formed request: $x")))
          }.getOrElse(List())

          // If the message is a status request. This method extract it and creates a response
          val statusResp = answerToStatusRequest(mess)

          // fixing the header, by adding a replyFrom key
          // The normal case is not to change the header, but to return the key-values as is
          // this may change in the futre
          val newH = mess.header + SPAttributes("replyFrom" -> API_OpcUARuntime.service, "replyID" -> ID.newID)

          // If it was a message to me with my api, i will reply here
          res.foreach{ body =>
            val replyMessage = APIParser.write(SPMessage(newH, body))
            if (mess.header.getAs[Boolean]("answerDirect").getOrElse(false))  // a special flag from the sender to answer directly to the sender via akka instead of the bus
              sender() ! replyMessage
            else {
              mediator ! Publish("answers", replyMessage)
            }
          }

          // If the message was a status request, we reply on the spevent bus
          statusResp.foreach{body =>
            val replyMessage = APIParser.write(SPMessage(newH, body))
            mediator ! Publish("spevents", replyMessage)
          }

        case Failure(err) => {}
      }
    case StateUpdate(activeState) =>
      val header = SPAttributes("replyFrom" -> API_OpcUARuntime.service, "replyID" -> ID.newID)
      val body = APIParser.writeJs(API_OpcUARuntime.StateUpdate(activeState, client.getCurrentTime.toString))
      val message = APIParser.write(SPMessage(header, body))
      mediator ! Publish("answers", message)
    case _ => sender ! APISP.SPError("Ill formed request")
  }

  // Matches if the message is to me
  // by cheking the header.to field and if the body is of my type.
  def getMyMessage(spMess : SPMessage) = {
    val to = spMess.header.getAs[String]("to").getOrElse("") // extracts the header.to, if it is to me
    val body = Try{APIParser.readJs[API_OpcUARuntime](spMess.body)}
    if (body.isSuccess && to == API_OpcUARuntime.service)
      Some(body.get)
    else
      None
  }

  val statusResponse = SPAttributes(
    "service" -> API_OpcUARuntime.service,
    "api" -> "to be added with macros later",
    "groups" -> List("examples"),
    "allowRequests" -> true
  )

  def answerToStatusRequest(spMess: SPMessage) = {
    val body = Try{APIParser.readJs[APISP.StatusRequest](spMess.body)}
    body.map{r =>
      APIParser.writeJs(
        APISP.StatusResponse(statusResponse)
      )
    }.toOption
  }
}
