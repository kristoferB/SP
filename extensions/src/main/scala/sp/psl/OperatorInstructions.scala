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
    "command" -> KeyDefinition("String", List("connect", "disconnect", "done"), Some("connect")),
    "doneID" -> KeyDefinition("ID", List(), Some(SPValue(ID.newID)))
  )
  val transformTuple  = (
    TransformValue("setup", _.getAs[BusSetup]("setup")),
    TransformValue("command", _.getAs[String]("command")),
    TransformValue("doneID", _.getAs[ID]("doneID"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)
  def props(eventHandler: ActorRef) = ServiceLauncher.props(Props(classOf[OperatorInstructions], eventHandler))
}

case class BusSetup(busIP: String, publishTopic: String, subscribeTopic: String)
case class SubscribeData(name: String, id: ID, address: String) // address is "run" and "mode"
case class OperatorInstructionData(id: ID, colors: List[String], textMessage: String)


// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class OperatorInstructions(eventHandler: ActorRef) extends Actor with ServiceSupport {
  import context.dispatcher
  val serviceID = ID.newID
  var theBus: Option[ActorRef] = None
  var setup: Option[BusSetup] = None
  var serviceName: Option[String] = None
  var subscriptions: Map[ID, SubscribeData] = Map()
  var run: Boolean = false // running
  var mode: Boolean = false  // finished

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
          val id = transform(OperatorInstructions.transformTuple._3)
          sendDone(id)
        case _ =>
          sender ! SPError("Ill formed request");
      }
      replyTo ! Response(List(), connectedAttribute(), service, serviceID)
    }
    case ConnectionEstablished(request, c) => {
      println("connected:"+request)
      setup.foreach{ s=>
        c ! ConsumeFromTopic(s.subscribeTopic)
        theBus = Some(c)
        eventHandler ! Response(List(), connectedAttribute(), serviceName.get, serviceID)
      }
    }
    case ConnectionFailed(request, reason) => {
      println("failed:"+reason)
    }
    case mess @ AMQMessage(body, prop, headers) => {
      val resp = SPAttributes.fromJson(body.toString).getOrElse(SPAttributes())
      println(s"new instruction on the bus")

      resp.getAs[String]("command") match {
        case Some("subscribe") =>
          resp.getAs[List[SubscribeData]]("data").foreach { oi =>
            oi.map(x => subscriptions += (x.id -> x))
            // reply with updated state
            sendState
          }
        case Some("write") =>
          resp.getAs[OperatorInstructionData]("data").foreach { oi =>
            mode = false
            run = true
            sendState
            eventHandler ! Response(List(), SPAttributes("operatorInstructions"->oi, "silent"->true), serviceName.get, serviceID)
          }
        case x@_ =>
          println("Unexpected " + x + " from " + body.toString)
      }
    }

    case ConnectionInterrupted(ca, x) => {
      println("connection closed")
      setup = None
    }
    case x => {
      println("no match for message: "+x)
    }
  }

  def sendState = {
    val data = subscriptions.map { case (k,v) =>
      val value =
        if(v.address == "mode") mode
        else if(v.address == "run") run
        else false
      SPAttributes("id" -> k, "value" -> value)
    }.toList
    val mess = SPAttributes("data" -> data)
    sendMessage(mess)
  }

  def setupBus(s: BusSetup, rnr: RequestNReply) = {
      setup = Some(s)
      serviceName = Some(rnr.req.service)
      println(s"connecting to $s")
      ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${s.busIP}:61616")
  }

  def connectedAttribute() = {
    if (setup.isEmpty)
      SPAttributes("theBus"->"Not connected") merge SPAttributes("silent"->true)
    else if (theBus.isEmpty)
      SPAttributes("theBus"->"Connecting") merge SPAttributes("silent"->true)
    else
      SPAttributes("theBus"->"Connected") merge SPAttributes("silent"->true)
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

  def sendDone(id: ID) = {
    mode = true
    run = false
    sendState
  }

  override def postStop() = {
    println("post stop, disconnect...")
    disconnect()
  }
}



