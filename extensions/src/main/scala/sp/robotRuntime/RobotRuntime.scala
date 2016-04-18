package sp.robotRuntime

import akka.actor._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.model._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages.{TransformValue, _}
import org.json4s.native.Serialization.{read, write}
import com.github.nscala_time.time.Imports._

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
      "kind" -> KeyDefinition("String", Commands.stringList, Some(Commands.Connect.toString))
    )
  )

  val transformValues = new {
    val command = TransformValue("command", _.getAs[Command]("command"))
    val setup = TransformValue("setup", _.getAs[BusSetup]("setup"))
    def list = List(command,setup)
  }

  val transformation = transformToList(transformValues.list)
  def props(eventHandler: ActorRef) = Props(classOf[RobotRuntime], eventHandler)
}

case class BusSetup(busIP: String, publishTopic: String, subscribeTopic: String)
case class Command(kind: Command)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class RobotRuntime(eventHandler: ActorRef) extends Actor with ServiceSupport {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization

  val serviceID = ID.newID
  var busSetup: Option[BusSetup] = None
  var bus: Option[ActorRef] = None
  var serviceName: Option[String] = None

  def receive = {
    case request @ Request(service, attr, ids, reqID) =>
      // Always include the following lines. Are used by the helper functions
      val theSender = sender()
      implicit val requestAndSender = RequestNReply(request, theSender)

      val command = transform(RobotRuntime.transformValues.command)
      println("Got command: " + command)

      import RobotRuntime.Commands._

      command.kind match {
        case Connect =>
          val setup = transform(RobotRuntime.transformValues.setup)
          connect(setup, requestAndSender)
        case Disconnect => disconnect()
        case Subscribe => subscribe()
        case Unsubscribe => unsubscribe()
      }

      theSender ! Response(List(), SPAttributes(), service, serviceID)

    case ConnectionEstablished(request, connection) =>
      println("Connected: " + request)
      busSetup.foreach { setup =>
        connection ! ConsumeFromTopic(setup.subscribeTopic)
        bus = Some(connection)
        eventHandler ! Progress(SPAttributes("connected" -> true), serviceName.get, serviceID)
      }

    case ConnectionFailed(request, reason) =>
      println("Failed: " + reason)

    case mess @ AMQMessage(body, prop, headers) =>
      //val robotEvent = SPAttributes.fromJson(body.toString)
      val robotEvent = modifyEvent(body.toString)
      println(s"We got: $robotEvent")
      //eventHandler ! Response(List(), guiEvent, serviceName.get, serviceID)
      eventHandler ! Response(List(), robotEvent, serviceName.get, serviceID)
      /* robotevent.get returns SPAttributes and sends to gui updater?
      * Transformations? What does the updater want?
      * If it wants it all send as is.
      */

    case ConnectionInterrupted(ca, x) =>
      println("Connection closed.")
      busSetup = None

    case x =>
      println("Robot runtime received a message it couldn't handle: " + x)
  }

  // added by Henrik
  def modifyEvent(jsonString: String): SPAttributes = {
    val extPp: IncomingPP = read[IncomingPP](jsonString)
    val extPpPos: PPEvent = extPp.PointerPosition
    val minPp: PPToGUI = PPToGUI(extPpPos.RobotId, extPpPos.Address, extPpPos.Data.Task,
      extPpPos.Data.Position.ModuleName, extPpPos.Data.Position.RoutineName, extPp.Instruction)
    val json = write(minPp)
    SPAttributes.fromJson(json).get
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
    bus.foreach(_ ! CloseConnection)
    this.busSetup = None
    this.bus = None
  }

  def subscribe() = {
    val mess = SPAttributes(
      "command" -> "subscribe"
    )
    sendMessage(mess)
    eventHandler ! Response(List(), SPAttributes(), serviceName.get, serviceID)
  }

  def unsubscribe() = {
    val mess = SPAttributes(
      "command" -> "unsubscribe"
    )
    sendMessage(mess)
  }

  def sendMessage(mess: SPAttributes) = {
    for {
      bus <- bus
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

// added by Henrik
case class IncomingPP(PointerPosition: PPEvent,
                      Instruction: String)

case class PPToGUI(RobotId: String,
                   Address: DataAddress,
                   Task: Task,
                   ModuleName: String,
                   RoutineName: String,
                   Instruction: String)

case class PPEvent(RobotId: String,
                   Address: DataAddress,
                   Data: PointerPositionData)

case class DataAddress (Domain: String,
                        Kind: String,
                        Path: String)

case class PointerPositionData (Task: Task,
                                Position: Position,
                                EventTime: DateTime)

case class Task (TaskName: String,
                 TaskType: Int,
                 Cycle: Int,
                 ExecutionType: Int,
                 ExecutionStatus: Int)

case class Position (ModuleName: String,
                     RoutineName: String,
                     Range: Range)

case class Range (Begin: Location,
                  End: Location)

case class Location (Column: Int,
                     Row: Int)