package sp.robotCycleAnalysis

import akka.actor._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.model._
import sp.domain.Logic._
import sp.domain.{SPAttributes, _}
import sp.system._
import sp.system.messages.{TransformValue, _}
import org.json4s.native.Serialization._
import com.github.nscala_time.time.Imports._
import org.json4s.JValue
import org.json4s.JsonAST.JNothing
import sp.robotCycleAnalysis.RobotCycleAnalysis.Commands.Command

object RobotCycleAnalysis extends SPService {
  object Commands extends Enumeration {
    type Command = Value
    val SetupBus = Value("setupBus")
    val ConnectToBus = Value("connectToBus")
    val DisconnectFromBus = Value("disconnectFromBus")
    val ListenToRobot = Value("listenToRobot")
    val StopListeningToRobot = Value("stopListeningToRobot")
    val GetRobotCycle = Value("getRobotCycle")
    val GetServiceState = Value("getServiceState")
    def stringList = this.values.toList.map(_.toString).asInstanceOf[List[SPValue]]
  }

  override val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "control" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "busSettings" -> SPAttributes(
      "host"  -> KeyDefinition("String", List(), Some("0.0.0.0")),
      "port"  -> KeyDefinition("Int", List(), Some(61616)),
      "topic" -> KeyDefinition("String", List(), Some("LISA"))
    ),
    "robot" -> SPAttributes(
      "name" -> KeyDefinition("String", List(), Some("unknown"))
    ),
    "command"   -> KeyDefinition("String", Commands.stringList, Some("getServiceState")),
    "cycle"   -> SPAttributes(
      "id" -> KeyDefinition("String", List(), Some("current"))
    )
  )

  case class TransformValues(busSettings: TransformValue[BusSettings],
                             robot: TransformValue[Robot],
                             command: TransformValue[String],
                             cycle: TransformValue[Cycle])

  val transformValues = TransformValues(
    TransformValue("busSettings", _.getAs[BusSettings]("busSettings")),
    TransformValue("robot", _.getAs[Robot]("robot")),
    TransformValue("command", _.getAs[String]("command")),
    TransformValue("cycle", _.getAs[Cycle]("cycle"))
  )

  override val transformation = transformToList(transformValues.productIterator.toList)
  def props(eventHandler: ActorRef) = ServiceLauncher.props(Props(classOf[RobotCycleAnalysis], eventHandler))
}

case class BusSettings(host: String, port: Int, topic: String)
case class Robot(name: String)
case class Cycle(id: String, routineStartsAndStops: Option[List[RoutineStartOrStop]])
case class RoutineStartOrStop(routine: Routine, start: Boolean, time: DateTime)
case class Routine(number: Int, name: String, description: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class RobotCycleAnalysis(eventHandler: ActorRef) extends Actor with ServiceSupport {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization

  val spServiceID = ID.newID
  val spServiceName = self.path.name
  var busSettings: Option[BusSettings] = None
  var bus: Option[ActorRef] = None

  var robotsListenedTo: List[Robot] = List.empty

  def receive = {
    case request @ Request(service, attr, ids, reqID) =>
      val theSender = sender()
      implicit val requestAndSender = RequestNReply(request, theSender)
      import RobotCycleAnalysis._
      import RobotCycleAnalysis.Commands._

      val commandString = transform(transformValues.command)
      val command = Commands.withName(commandString)

      println("Got command: " + command)

      command match {
        case SetupBus =>
          busSettings = Some(transform(transformValues.busSettings))
          val mess = SPAttributes(
            "busSettings" -> busSettings
          )
          sendViaSSE(mess)
          answerOK(sender)
        case ConnectToBus =>
          ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${busSettings.get.host}:${busSettings.get.port}")
          answerOK(sender)
        case DisconnectFromBus =>
          disconnectFromBus()
          answerOK(sender)
        case ListenToRobot =>
          val robot = transform(transformValues.robot)
          robotsListenedTo = robot :: robotsListenedTo
          val mess = SPAttributes(
            "robot" -> robot,
            "isListenedTo" -> true
          )
          sendViaSSE(mess)
          answerOK(sender)
        case StopListeningToRobot =>
          val robot = transform(transformValues.robot)
          robotsListenedTo = robotsListenedTo.filter(_.name != robot.name)
          val mess = SPAttributes(
            "robot" -> robot,
            "isListenedTo" -> false
          )
          sendViaSSE(mess)
          answerOK(sender)
        case GetServiceState =>
          val state = SPAttributes(
            "busSettings" -> busSettings,
            "busConnected" -> bus.isDefined,
            "robotsListenedTo" -> robotsListenedTo
          )
          theSender ! Response(List(), state, spServiceName, spServiceID)
        case GetRobotCycle =>
          val robot = transform(transformValues.robot)
          val cycle = transform(transformValues.cycle)
          val mess = SPAttributes(
            "robot" -> robot,
            "cycle" -> cycle
          )
          sendToBus(mess)
          answerOK(sender)
      }

    case ConnectionEstablished(request, busConnection) =>
      busSettings.foreach { setup =>
        busConnection ! ConsumeFromTopic(setup.topic)
        bus = Some(busConnection)
        println("Bus connection established: " + request)
        notifyConnectionStatus()
      }

    case ConnectionFailed(request, reason) =>
      println("Bus connection failed: " + reason)
      notifyError("BusConnectionFailed")

    case AMQMessage(body, prop, headers) =>
      import org.json4s._
      import org.json4s.native.JsonMethods._

      case class RoutineEventMessage(robot: Robot, routineStartOrStop: RoutineStartOrStop)

      val json = body.toString
      val jObject = parse(json)

      if (jObject.has("robot")) {
        if (jObject.has("cycle") && (jObject \ "cycle").has("routineStartsAndStops") ) {
          sendViaSSE(SPAttributes.fromJson(json).get)
        } else if (jObject.has("routineStartOrStop")) {
          if (robotsListenedTo.exists(r => r.name == (jObject \ "robot" \ "name").to[String].get))
            sendViaSSE(SPAttributes.fromJson(json).get)
        }
      }

    case ConnectionInterrupted(ca, x) =>
      bus = None
      notifyConnectionStatus()
      notifyError("BusConnectionInterrupted")
      println("Bus connection interrupted.")

    case x =>
      println("RobotCycleAnalysis service received a message it couldn't handle: " + x)
  }

  def answerOK(sender: ActorRef): Unit = {
    answer(sender, SPAttributes())
  }

  def answer(sender: ActorRef, message: SPAttributes): Unit = {
    sender ! Response(List(), message, spServiceName, spServiceID)
  }

  def sendViaSSE(message: SPAttributes): Unit = {
    eventHandler ! Response(List(), message, spServiceName, spServiceID)
  }

  def notifyConnectionStatus(): Unit = {
    eventHandler ! Progress(SPAttributes("busConnected" -> bus.isDefined), spServiceName, spServiceID)
  }

  def notifyError(error: String): Unit = {
    eventHandler ! ServiceError(spServiceName, spServiceID, SPErrorString(error))
  }

  def sendToBus(mess: SPAttributes) = {
    for {
      bus <- bus
      s <- busSettings
    } yield {
      println(s"sending: ${mess.toJson}")
      bus ! SendMessage(Topic(s.topic), AMQMessage(mess.toJson))
    }
  }

  def disconnectFromBus(): Unit = {
    bus.foreach(_ ! CloseConnection)
    bus = None
  }

  override def postStop() = {
    disconnectFromBus()
  }

  implicit class JValueExtended(value: JValue) {
    def has(childString: String): Boolean = {
      if ((value \ childString) != JNothing) {
        true
      } else {
        false
      }
    }
  }

}