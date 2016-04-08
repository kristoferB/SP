package sp.control

import akka.actor._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.model._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._

object RobotRuntime extends SPService {
  object Commands extends Enumeration {
    type Command = Value
    val Connect = Value("connect")
    val Disconnect = Value("disconnect")
    val Subscribe = Value("subscribe")
    val Unsubscribe = Value("unsubscribe")
    def stringList = this.values.toList.map(_.toString).asInstanceOf[List[SPValue]]
  }

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "control" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> SPAttributes(
      "busIP"          -> KeyDefinition("String", List(), Some("0.0.0.0")),
      "publishTopic"   -> KeyDefinition("String", List(), Some("commands")),
      "subscribeTopic" -> KeyDefinition("String", List(), Some("response"))
    ),
    "connection" -> SPAttributes(
      "robotDataPoint" -> KeyDefinition("String", List(), None)
    ),
    "command" -> SPAttributes(
      "commandType"    -> KeyDefinition("String", Commands.stringList, Some(Commands.Connect.toString))
    )
  )

  val transformValues  = (
    TransformValue("setup", _.getAs[BusSetup]("setup")),
    TransformValue("command", _.getAs[SPAttributes]("command"))
  )

  val transformation = transformToList(transformValues.productIterator.toList)
  def props(eventHandler: ActorRef) = Props(classOf[OperationControl], eventHandler)
}

case class BusSetup(busIP: String, publishTopic: String, subscribeTopic: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class RobotRuntime(eventHandler: ActorRef) extends Actor with ServiceSupport {
  val serviceID = ID.newID
  var busSetup: Option[BusSetup] = None
  var theBus: Option[ActorRef] = None
  var serviceName: Option[String] = None

  def receive = {
    case r @ Request(service, attr, ids, reqID) =>
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val requestNReply = RequestNReply(r, replyTo)

      val setup = transform(RobotRuntime.transformValues._1)
      val commands = transform(RobotRuntime.transformValues._2)

      val commandType = commands.getAs[String]("commandType").get
      println("CommandType: " + commandType)

      import RobotRuntime.Commands._

      RobotRuntime.Commands.withName(commandType) match {
        case Connect => connect(setup, requestNReply)
        case Disconnect => disconnect()
        case Subscribe => subscribe()
        case Unsubscribe => unsubscribe()
      }

      replyTo ! Response(List(), SPAttributes(), service, serviceID)

    case ConnectionEstablished(request, c) =>
      println("connected: " + request)
      busSetup.foreach{ s =>
        c ! ConsumeFromTopic(s.subscribeTopic)
        theBus = Some(c)
        eventHandler ! Progress(SPAttributes("theBus" -> "Connected"), serviceName.get, serviceID)
      }

    case ConnectionFailed(request, reason) =>
      println("Failed: " + reason)

    case mess @ AMQMessage(body, prop, headers) =>
      val resp = SPAttributes.fromJson(body.toString)
      println(s"We got: $resp")
      for {
        m <- resp
        list <- m.getAs[List[SPAttributes]]("dbs")
      } yield for {
        l <- list
        id <- l.getAs[ID]("id")
        value <- l.getAs[SPValue]("value")
      } yield {

      }
      eventHandler ! Response(List(), SPAttributes(), serviceName.get, serviceID)

    case ConnectionInterrupted(ca, x) =>
      println("Connection closed.")
      busSetup = None

    case x =>
      println("Robot runtime received a message it couldn't handle: " + x)
  }

  def connect(setup: BusSetup, rnr: RequestNReply) = {
      busSetup = Some(setup)
      serviceName = Some(rnr.req.service)
      println(s"Connecting: $setup")
      ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${setup.busIP}:61616")
  }

  def disconnect() = {
    println("Disconnecting from bus.")
    unsubscribe()
    theBus.foreach(_ ! CloseConnection)
    this.busSetup = None
    this.theBus = None
  }

  def subscribe() = {
    val mess = SPAttributes(
      "command"->"subscribe"
    )
    sendMessage(mess)
    eventHandler ! Response(List(), SPAttributes(), serviceName.get, serviceID)
  }

  def unsubscribe() = {
    val mess = SPAttributes(
      "command"->"unsubscribe"
    )
    sendMessage(mess)
  }

  def sendCommands(commands: SPAttributes) = {
    commands.getAs[ID]("execute").foreach { id =>

      val command = SPAttributes(
        "id" -> id
      )

      val mess = SPAttributes(
        "commands" -> List(command),
        "command" -> "write"
      )

      sendMessage(mess)
    }
  }

  def sendMessage(mess: SPAttributes) = {
    for {
      bus <- theBus
      s <- busSetup
    } yield {
      println(s"sending: ${mess.toJson}")
      bus ! SendMessage(Topic(s.publishTopic), AMQMessage(mess.toJson))
    }
  }

  override def postStop() = {
    disconnect()
  }
}


