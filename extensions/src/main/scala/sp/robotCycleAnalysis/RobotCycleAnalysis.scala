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
import org.json4s.{JValue, JsonAST}
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
  def props(eventHandler: ActorRef) = Props(classOf[RobotCycleAnalysis], eventHandler)
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
  var connectionInterrupted = false

  var liveWorkCells: List[WorkCell] = List.empty
  var availableWorkCells: List[WorkCell] = List.empty

  def receive = {
    case RegisterService(s, _, _,_) => println(s"Service $s is registered")
    case request @ Request(service, attr, ids, reqID) =>
      val theSender = sender()
      implicit val requestAndSender = RequestNReply(request, theSender)
      import RobotCycleAnalysis._
      import RobotCycleAnalysis.Commands._

      val commandString = transform(transformValues.command)
      val command = Commands.withName(commandString)
      var response: Option[Response] = None

      println("Got command: " + command)

      command match {
        case SetupBus =>
          busSettings = Some(transform(transformValues.busSettings))
          val mess = SPAttributes(
            "busSettings" -> busSettings
          )
          sendViaSSE(mess)
        case ConnectToBus =>
          val result = for {
            b <- busSettings.getOrError("Bus settings not complete.")
          } yield
            ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${b.host}:${b.port}")
          notifyIfError("Failed to connect to the bus", result)
        case DisconnectFromBus =>
          disconnectFromBus()
        case StartLiveWatch =>
          val workCell = transform(transformValues.workCell)
          liveWorkCells = workCell :: liveWorkCells
          val mess = SPAttributes(
            "addedLiveWatch" -> workCell
          )
          sendViaSSE(mess)
        case StopLiveWatch =>
          val workCell = transform(transformValues.workCell)
          liveWorkCells = liveWorkCells.filter(w => w.name != workCell.name)
          val mess = SPAttributes(
            "removedLiveWatch" -> workCell
          )
          sendViaSSE(mess)
        case GetServiceState =>
          val mess = SPAttributes(
            "availableWorkCells" -> availableWorkCells,
            "busSettings" -> busSettings,
            "busConnected" -> bus.isDefined,
            "connectionInterrupted" -> connectionInterrupted,
            "liveWorkCells" -> liveWorkCells
          )
          answer(theSender, mess)
        case GetAvailableCycles =>
          val workCell = transform(transformValues.workCell)
          val timeSpan = transform(transformValues.timeSpan)
          val mess = SPAttributes(
            "availableCycles" -> null,
            "timeSpan" -> timeSpan,
            "workCell" -> workCell
          )
          sendToBus(mess)
        case GetAvailableWorkCells =>
          val mess = SPAttributes(
            "availableWorkCells" -> availableWorkCells
          )
          response = Some(Response(List(), mess, spServiceName, spServiceID))
        case GetCycle =>
          val cycle = transform(transformValues.cycle)
          val mess = SPAttributes(
            "cycle" -> cycle
          )
          sendToBus(mess)
      }

      response match {
        case Some(r) => theSender ! r
        case None    => answer(theSender, SPAttributes())
      }

    case ConnectionEstablished(request, busConnection) =>
      bus = Some(busConnection)
      println("Bus connection established: " + request)
      busSettings.foreach { setup =>
        busConnection ! ConsumeFromTopic(setup.topic)
        notifyConnectionStatus()
        val mess = SPAttributes(
          "availableWorkCells" -> null
        )
        sendToBus(mess)
      }

    case AMQMessage(body, prop, headers) =>
      import org.json4s._
      import org.json4s.native.JsonMethods._

      val json = body.toString
      val jObject = parse(json)

      val isAvailableCycles = jObject.has("workCell") && jObject.has("availableCycles")
      val isCycle = jObject.has("cycle") && (jObject \ "cycle").has("events")
      val isCycleEvent = jObject.has("cycleEvent") &&
        liveWorkCells.exists(w => w.name == (jObject \ "cycleEvent" \ "workCell" \ "name").to[String].get)
      val isAvailableWorkCells = jObject.has("availableWorkCells") && (jObject \ "availableWorkCells") != JNull

      if (isCycle || isAvailableCycles || isCycleEvent) {
        val spAttributes = SPAttributes.fromJson(json).get
        sendViaSSE(spAttributes)
      } else if (isAvailableWorkCells) {
        case class AvailableWorkCells(availableWorkCells: List[WorkCell])
        availableWorkCells = read[AvailableWorkCells](json).availableWorkCells
      }

    case ConnectionInterrupted(ca, x) =>
      connectionInterrupted = true
      notifyConnectionStatus()

    case ConnectionReestablished =>
      connectionInterrupted = false
      notifyConnectionStatus()

    case ConnectionFailed(request, reason) => notifyError(reason.getMessage)
    case SendAck => Unit
    case x => println("RobotCycleAnalysis service received a message it couldn't handle: " + x)
  }

  def answer(sender: ActorRef, message: SPAttributes): Unit = {
    sender ! Response(List(), message, spServiceName, spServiceID)
  }

  def sendViaSSE(message: SPAttributes): Unit = {
    eventHandler ! Response(List(), message, spServiceName, spServiceID)
  }

  def notifyConnectionStatus(): Unit = {
    val mess = SPAttributes(
      "busConnected" -> bus.isDefined,
      "connectionInterrupted" -> connectionInterrupted
    )
    eventHandler ! Progress(mess, spServiceName, spServiceID)
  }

  def notifyError(error: String): Unit = {
    eventHandler ! ServiceError(spServiceName, spServiceID, SPErrorString(error))
  }

  def notifyIfError[T](generalDescription: String, result: Either[SPErrorString, T]) {
    if (result.isLeft) {
      val exactlyWhatWentWrong = result.left.get.error
      notifyError(generalDescription + ": " + exactlyWhatWentWrong)
    }
  }

  def sendToBus(mess: SPAttributes) = {
    val result = for {
      bus <- bus.getOrError("You are not connected to any bus.")
      s <- busSettings.getOrError("There are missing bus settings.")
    } yield {
      println(s"sending: ${mess.toJson}")
      bus ! SendMessage(Topic(s.topic), AMQMessage(mess.toJson))
    }
    notifyIfError("Failed to send message to bus", result)
  }

  def disconnectFromBus(): Unit = {
    bus.foreach(_ ! CloseConnection)
    bus = None
    notifyConnectionStatus()
  }

  override def postStop() = {
    disconnectFromBus()
  }

  implicit class OptionExtended[T](option: Option[T]) {
    def getOrError(error: String): Either.RightProjection[SPErrorString, T] = {
      val either = option match {
        case Some(x) => Right(option.get)
        case None => Left(SPErrorString(error))
      }
      either.right
    }
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