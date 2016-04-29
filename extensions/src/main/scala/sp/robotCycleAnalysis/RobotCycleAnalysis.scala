package sp.robotCycleAnalysis

import akka.actor._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.model._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages.{TransformValue, _}
import org.json4s.native.Serialization._
import com.github.nscala_time.time.Imports._

object RobotCycleAnalysis extends SPService {
  object Commands extends Enumeration {
    type Command = Value
    val SetupBus = Value("setupBus")
    val ConnectToBus = Value("connectToBus")
    val DisconnectFromBus = Value("disconnectFromBus")
    val ListenToRobot = Value("listenToRobot")
    val StopListenToRobot = Value("stopListenToRobot")
    val GetRobotCycle = Value("getRobotCycle")
    def stringList = this.values.toList.map(_.toString).asInstanceOf[List[SPValue]]
  }

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "control" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "busSettings" -> SPAttributes(
      "host"  -> KeyDefinition("String", List(), Some("0.0.0.0")),
      "port"  -> KeyDefinition("Int", List(), Some(61616)),
      "topic" -> KeyDefinition("String", List(), Some("LISA"))
    ),
    "robot" -> SPAttributes(
      "name" -> KeyDefinition("String", List(), None)
    ),
    "command" -> SPAttributes(
      "kind" -> KeyDefinition("String", Commands.stringList, None)
    )
  )

  val transformValues = new {
    val command = TransformValue("command", _.getAs[Command]("command"))
    val busSettings = TransformValue("busSettings", _.getAs[BusSettings]("busSettings"))
    val robot = TransformValue("robot", _.getAs[Robot]("robot"))
    def list = List(command, busSettings, robot)
  }

  val transformation = transformToList(transformValues.list)
  def props(eventHandler: ActorRef) = Props(classOf[RobotCycleAnalysis], eventHandler)
}

case class BusSettings(host: String, port: Int, topic: String)
case class Command(kind: Command)
case class Robot(name: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class RobotCycleAnalysis(eventHandler: ActorRef) extends Actor with ServiceSupport {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization

  val spServiceID = ID.newID
  val spServiceName = self.path.name
  var busSetup: Option[BusSettings] = None
  var bus: Option[ActorRef] = None
  var robotListenedTo: Option[Robot] = None

  def receive = {
    case request @ Request(service, attr, ids, reqID) =>
      // Always include the following lines. Are used by the helper functions
      val theSender = sender()
      implicit val requestAndSender = RequestNReply(request, theSender)

      val command = transform(RobotCycleAnalysis.transformValues.command)
      println("Got command: " + command)

      import RobotCycleAnalysis.Commands._

      command.kind match {
        case SetupBus =>
          val setup = transform(RobotCycleAnalysis.transformValues.busSettings)
          busSetup = Some(setup)
        case ConnectToBus => ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${busSetup.get.host}:${busSetup.get.port}")
        case DisconnectFromBus => disconnectFromBus()
        case ListenToRobot =>
          val robot = transform(RobotCycleAnalysis.transformValues.robot)
          robotListenedTo = Some(robot)
        case StopListenToRobot =>
          robotListenedTo = None
      }

      theSender ! SPOK()

    case ConnectionEstablished(request, busConnection) =>
      println("Connected: " + request)
      busSetup.foreach { setup =>
        busConnection ! ConsumeFromTopic(setup.topic)
        bus = Some(busConnection)
        eventHandler ! Progress(SPAttributes("connected" -> true), spServiceName, spServiceID)
      }

    case ConnectionFailed(request, reason) =>
      println("failed:"+reason)
      eventHandler ! ServiceError(spServiceName, spServiceID, SPErrorString("ConnectionFailed"))

    case busMessage @ AMQMessage(body, prop, headers) =>
      val event = read[RobotProgramEvent](body.toString)
      println(s"We got: $event")
      val eventAsJson = write(event)
      val eventAsSPAttributes = SPAttributes.fromJson(eventAsJson).get
      eventHandler ! Response(List(), eventAsSPAttributes, spServiceName, spServiceID)

    case ConnectionInterrupted(ca, x) =>
      eventHandler ! ServiceError(spServiceName, spServiceID, SPErrorString("ConnectionInterrupted"))
      println("Connection closed.")
      busSetup = None

    case x =>
      println("Robot runtime received a message it couldn't handle: " + x)
  }

  def disconnectFromBus() = {
    bus.foreach(_ ! CloseConnection)
    this.busSetup = None
    this.bus = None
  }

  def sendMessage(mess: SPAttributes) = {
    for {
      bus <- bus
      s <- busSetup
    } yield {
      println(s"sending: ${mess.toJson}")
      bus ! SendMessage(Topic(s.topic), AMQMessage(mess.toJson))
    }
  }

  override def postStop() = {
    disconnectFromBus()
  }

}

trait RobotProgramEvent
case class RobotProgramStart(controllerName: String, startedProgram: RobotProgram) extends RobotProgramEvent
case class RobotProgramEnd(controllerName: String, endedProgram: RobotProgram) extends RobotProgramEvent
case class RobotProgram(number: Int, name: String, description: String)