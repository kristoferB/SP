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
import org.json4s.JsonAST.{JArray, JNothing}
import sp.robotCycleAnalysis.RobotCycleAnalysis.Commands.Command

object RobotCycleAnalysis extends SPService {
  object Commands extends Enumeration {
    type Command = Value
    val SetupBus = Value("setupBus")
    val ConnectToBus = Value("connectToBus")
    val DisconnectFromBus = Value("disconnectFromBus")
    val StartLiveWatch = Value("startLiveWatch")
    val StopLiveWatch = Value("stopLiveWatch")
    val GetCycle = Value("getCycle")
    val GetAvailableCycles = Value("getAvailableCycles")
    val GetAvailableWorkCells = Value("getAvailableWorkCells")
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
    "workCell" -> SPAttributes(
      "name" -> KeyDefinition("String", List(), Some("not set"))
    ),
    "timeSpan" -> SPAttributes(
      "start" -> KeyDefinition("String", List(), Some("not set")),
      "stop" -> KeyDefinition("String", List(), Some("not set"))
    ),
    "command" -> KeyDefinition("String", Commands.stringList, Some("getServiceState")),
    "cycle"   -> SPAttributes(
      "id"      -> KeyDefinition("Option[String]", List(), None),
      "current" -> KeyDefinition("Option[Boolean]", List(), None)
    )
  )

  case class TransformValues(busSettings: TransformValue[BusSettings],
                             workCell: TransformValue[WorkCell],
                             timeSpan: TransformValue[TimeSpan],
                             command: TransformValue[String],
                             cycle: TransformValue[Cycle])

  val transformValues = TransformValues(
    TransformValue("busSettings", _.getAs[BusSettings]("busSettings")),
    TransformValue("workCell", _.getAs[WorkCell]("workCell")),
    TransformValue("timeSpan", _.getAs[TimeSpan]("timeSpan")),
    TransformValue("command", _.getAs[String]("command")),
    TransformValue("cycle", _.getAs[Cycle]("cycle"))
  )

  override val transformation = transformToList(transformValues.productIterator.toList)
  def props(eventHandler: ActorRef) = ServiceLauncher.props(Props(classOf[RobotCycleAnalysis], eventHandler))
}

case class BusSettings(host: String, port: Int, topic: String)
case class Robot(name: String)
case class WorkCell(name: String, robots: Option[List[Robot]])
case class TimeSpan(start: String, stop: String)
case class Cycle(id: Option[ID], workCell: Option[WorkCell], events: Option[Map[String, List[CycleEvent]]])
trait CycleEvent { def start: Boolean; def time: DateTime; }
case class RoutineStartOrStop(start: Boolean, time: DateTime, robot: Robot, routine: Routine) extends CycleEvent
case class CycleStartOrStop(start: Boolean, time: DateTime) extends CycleEvent
case class Routine(number: Int, name: String, description: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class RobotCycleAnalysis(eventHandler: ActorRef) extends Actor with ServiceSupport {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization

  val spServiceID = ID.newID
  val spServiceName = self.path.name
  var busSettings: Option[BusSettings] = None
  var bus: Option[ActorRef] = None

  var liveWorkCells: List[WorkCell] = List.empty
  var availableWorkCells: List[WorkCell] = List.empty

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
        case StartLiveWatch =>
          val workCell = transform(transformValues.workCell)
          liveWorkCells = workCell :: liveWorkCells
          val mess = SPAttributes(
            "addedLiveWatch" -> workCell
          )
          sendViaSSE(mess)
          answerOK(sender)
        case StopLiveWatch =>
          val workCell = transform(transformValues.workCell)
          liveWorkCells = liveWorkCells.filter(w => w.name != workCell.name)
          val mess = SPAttributes(
            "removedLiveWatch" -> workCell
          )
          sendViaSSE(mess)
          answerOK(sender)
        case GetServiceState =>
          val mess = SPAttributes(
            "busSettings" -> busSettings,
            "busConnected" -> bus.isDefined,
            "liveWorkCells" -> liveWorkCells,
            "availableWorkCells" -> availableWorkCells
          )
          theSender ! Response(List(), mess, spServiceName, spServiceID)
        case GetAvailableCycles =>
          val workCell = transform(transformValues.workCell)
          val timeSpan = transform(transformValues.timeSpan)
          val mess = SPAttributes(
            "workCell" -> workCell,
            "timeSpan" -> timeSpan,
            "availableCycles" -> null
          )
          sendToBus(mess)
          answerOK(sender)
        case GetAvailableWorkCells =>
          val mess = SPAttributes(
            "availableWorkCells" -> availableWorkCells
          )
          theSender ! Response(List(), mess, spServiceName, spServiceID)
        case GetCycle =>
          val cycle = transform(transformValues.cycle)
          val mess = SPAttributes(
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
        val mess = SPAttributes(
          "availableWorkCells" -> null
        )
        sendToBus(mess)
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
      val spAttributes = SPAttributes.fromJson(json).get

      val isAvailableCycles = jObject.has("workCell") && jObject.has("availableCycles")
      val isCycle = jObject.has("cycle") && (jObject \ "cycle").has("events")
      val isCycleEvent = jObject.has("cycleEvent") &&
        liveWorkCells.exists(w => w.name == (jObject \ "cycleEvent" \ "workCell" \ "name").to[String].get)
      val isAvailableWorkCells = jObject.has("availableWorkCells")

      if (isCycle || isAvailableCycles || isCycleEvent)
        sendViaSSE(spAttributes)
      else if (isAvailableWorkCells) {
        availableWorkCells = read[List[WorkCell]](json)
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