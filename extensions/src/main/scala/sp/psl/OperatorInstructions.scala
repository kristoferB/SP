package sp.psl

import akka.actor._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._
import org.json4s.JsonAST.JInt
import sp.domain.logic.IDAbleLogic
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._

import scala.util.Try


object OperatorInstructions extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "psl" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> SPAttributes(
      "busIP" -> KeyDefinition("String", List(), Some("129.16.26.22")),
      "publishTopic" -> KeyDefinition("String", List(), Some("commands")),
      "subscribeTopic" -> KeyDefinition("String", List(), Some("response"))
    ),
    "command" -> KeyDefinition("String", List("connect", "disconnect", "done"), Some("connect")
    )
  )
  val transformTuple  = (
    TransformValue("setup", _.getAs[BusSetup]("setup")),
    TransformValue("command", _.getAs[String]("command"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)
  def props(eventHandler: ActorRef) = ServiceLauncher.props(Props(classOf[OperatorInstructions], eventHandler))
}

case class BusSetup(busIP: String, publishTopic: String, subscribeTopic: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class OperatorInstructions(eventHandler: ActorRef) extends Actor with ServiceSupport {
  import context.dispatcher
  val serviceID = ID.newID
  var theBus: Option[ActorRef] = None
  var setup: Option[BusSetup] = None
  var serviceName: Option[String] = None

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val s = transform(OperatorInstructions.transformTuple._1)
      val command= transform(OperatorInstructions.transformTuple._2)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      println("Command: " + command)

      command match {
        case "connect" =>
          setupBus(s, rnr)
        case "disconnect" =>
          disconnect()
        case "done" =>
          sendDone()
        case _ =>
          println("no such command")
      }
      replyTo ! Response(List(), connectedAttribute() merge SPAttributes("silent"->true), service, serviceID)
    }
    case ConnectionEstablished(request, c) => {
      println("connected:"+request)
      setup.foreach{ s=>
        c ! ConsumeFromTopic(s.subscribeTopic)
        theBus = Some(c)
        eventHandler ! Progress(SPAttributes("theBus"-> "Connected"), serviceName.get, serviceID)
      }
    }
    case ConnectionFailed(request, reason) => {
      println("failed:"+reason)
    }
    case mess @ AMQMessage(body, prop, headers) => {
      val resp = SPAttributes.fromJson(body.toString)
      println(s"new instruction on the bus")
      val colors = resp.flatMap(_.getAs[List[String]]("operator_instructions"))
      val opinstr = SPAttributes("colors" -> colors, "textMessage" -> "Please do this")
      eventHandler ! Response(List(), SPAttributes("operatorInstructions"->opinstr, "silent"->true), serviceName.get, serviceID)
    }
    case ConnectionInterrupted(ca, x) => {
      println("connection closed")
      setup = None
    }
    case x => {
      println("no match for message: "+x)
    }
  }

  def setupBus(s: BusSetup, rnr: RequestNReply) = {
      setup = Some(s)
      serviceName = Some(rnr.req.service)
      println(s"connecting to $s")
      ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${s.busIP}:61616")
  }

  def connectedAttribute() = {
    if (setup.isEmpty)
      SPAttributes("theBus"->"Not connected")
    else if (theBus.isEmpty)
      SPAttributes("theBus"->"Connecting")
    else
      SPAttributes("theBus"->"Connected")
  }

  def disconnect() = {
    println("disconnecting from the bus")
    theBus.foreach(_ ! CloseConnection)
    this.setup = None
    this.theBus = None
  }

  def sendMessage(mess: SPAttributes) = {
    for {
      bus <- theBus
      s <- setup
    } yield {
      println(s"sending: ${mess.toJson}")
      bus ! SendMessage(Topic(s.publishTopic), AMQMessage(mess.toJson))
    }
  }

  def sendDone() = {
    val mess = SPAttributes(
      "command"->"operatorDone"
    )
    sendMessage(mess)
  }

  override def postStop() = {
    println("post stop, disconnect...")
    disconnect()
  }
}



