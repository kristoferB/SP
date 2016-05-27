package sp.robotCycleAnalysis

import java.util.concurrent.TimeUnit

import akka.actor._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.model._
import sp.domain.Logic._
import sp.domain.{SPAttributes, _}
import sp.system._
import sp.system.messages.{TransformValue, _}
import sp.system.SPActorSystem._
import com.github.nscala_time.time.Imports._
import org.json4s.JValue
import org.json4s.JsonAST.{JBool, JNothing}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration

object RobotCycleAnalysis extends SPService {
  object Commands extends Enumeration {
    type Command = Value
    val SetupBus = Value("setupBus")
    val ConnectToBus = Value("connectToBus")
    val DisconnectFromBus = Value("disconnectFromBus")
    val StartLiveWatch = Value("startLiveWatch")
    val StopLiveWatch = Value("stopLiveWatch")
    val SearchCycles = Value("searchCycles")
    val RequestAvailableWorkCells = Value("requestAvailableWorkCells")
    val GetServiceState = Value("getServiceState")
    def stringList = this.values.toList.map(_.toString).asInstanceOf[List[SPValue]]
  }

  override val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "control" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "busSettings" -> SPAttributes(
      "host"  -> KeyDefinition("String", List(), Some(settings.activeMQ)),
      "port"  -> KeyDefinition("Int", List(), Some(settings.activeMQPort)),
      "topic" -> KeyDefinition("String", List(), Some(settings.activeMQTopic))
    ),
    "workCellId" -> KeyDefinition("String", List(), Some("not set")),
    "timeSpan" -> SPAttributes(
      "from" -> KeyDefinition("String", List(), Some("not set")),
      "to" -> KeyDefinition("String", List(), Some("not set"))
    ),
    "command" -> KeyDefinition("String", Commands.stringList, Some("getServiceState")),
    "cycleId" -> KeyDefinition("String", List(), Some("not set"))
  )

  case class TransformValues(busSettings: TransformValue[BusSettings],
                             workCellId: TransformValue[String],
                             timeSpan: TransformValue[TimeSpan],
                             command: TransformValue[String],
                             cycleId: TransformValue[String])

  val transformValues = TransformValues(
    TransformValue("busSettings", _.getAs[BusSettings]("busSettings")),
    TransformValue("workCellId", _.getAs[String]("workCellId")),
    TransformValue("timeSpan", _.getAs[TimeSpan]("timeSpan")),
    TransformValue("command", _.getAs[String]("command")),
    TransformValue("cycleId", _.getAs[String]("cycleId"))
  )

  override val transformation = transformToList(transformValues.productIterator.toList)
  def props(eventHandler: ActorRef) = Props(classOf[RobotCycleAnalysis], eventHandler)
}

case class RobotCyclesRequest(cycleIds: Option[List[String]])
case class BusSettings(host: String, port: Int, topic: String)
case class Robot(name: String)
case class TimeSpan(from: String, to: String)
trait CycleEvent { def start: Boolean; def time: DateTime; }
case class RoutineStartOrStop(start: Boolean, time: DateTime, robot: Robot, routine: Routine) extends CycleEvent
case class CycleStartOrStop(start: Boolean, time: DateTime) extends CycleEvent
case class Routine(number: Int, name: String, description: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class RobotCycleAnalysis(eventHandler: ActorRef) extends Actor with ServiceSupport {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization

  val spServiceID = ID.newID
  val spServiceName = self.path.name
  var busSettings: Option[BusSettings] = Some(BusSettings(settings.activeMQ, settings.activeMQPort,
    settings.activeMQTopic))
  var bus: Option[ActorRef] = None
  var isInterrupted = false
  var liveWorkCells: List[String] = List.empty

  import scala.concurrent.ExecutionContext.Implicits.global
  // Test schedule
  var latestActivityId: Option[ID] = None
  system.scheduler.schedule(FiniteDuration(0L, TimeUnit.SECONDS), FiniteDuration(5L, TimeUnit.SECONDS))(fakeRobotEventToBus)

  def fakeRobotEventToBus = {
    val activityId =
      if (latestActivityId.isDefined) {
        val id = latestActivityId.get
        latestActivityId = None
        id
      }
      else {
        val id = ID.newID
        latestActivityId = Some(id)
        id
      }

    val mess = SPAttributes(
      "robotEvent" -> SPAttributes(
        "workCellId" -> "117956",
        "activityId" -> activityId,
        "activityType" -> "routines",
        "name" -> "Moving",
        "isStart" -> latestActivityId.isDefined,
        "robotId" -> "R2",
        "time" -> DateTime.now
      )
    )
    if (bus.isDefined)
      sendToBus(mess)
  }

  def receive = {
    case RegisterService(s, _, _,_) => println(s"Service $s is registered")
    case request @ Request(service, attr, ids, reqID) =>
      val theSender = sender()
      implicit val requestAndSender = RequestNReply(request, theSender)
      import RobotCycleAnalysis._
      import RobotCycleAnalysis.Commands._

      val postedBusSettings = transform(transformValues.busSettings)
      val postedCycleId = transform(transformValues.cycleId)
      val postedTimeSpan = transform(transformValues.timeSpan)
      val postedWorkCellId = transform(transformValues.workCellId)

      val commandString = transform(transformValues.command)
      val command = Commands.withName(commandString)
      var answer: Option[SPEvent] = None

      println("Got command: " + command)

      command match {
        case ConnectToBus => answer = connectToBus()
        case DisconnectFromBus => answer = disconnectFromBus()
        case GetServiceState => answer = getServiceState
        case RequestAvailableWorkCells => requestAvailableWorkCells()
        case SearchCycles => searchCycles(postedCycleId, postedTimeSpan, postedWorkCellId)
        case SetupBus => setupBus(postedBusSettings)
        case StartLiveWatch => startLiveWatch(postedWorkCellId)
        case StopLiveWatch => stopLiveWatch(postedWorkCellId)
      }

      answer match {
        case Some(a) => theSender ! a
        case None    => theSender ! okResponse()
      }

    case ConnectionEstablished(request, busConnection) => connectionEstablished(request, busConnection)
    case AMQMessage(body, prop, headers) => amqMessage(body, prop, headers)
    case ConnectionInterrupted(ca, x) => connectionInterrupted(ca, x)
    case ConnectionReestablished => connectionReestablished()
    case ConnectionFailed(request, reason) => eventHandler ! SPErrorString(reason.getMessage)
    case SendAck => Unit
    case x => println("RobotCycleAnalysis service received a message it couldn't handle: " + x)
  }

  // --- Handlers for the different cases above --- //

  def setupBus(postedBusSettings: BusSettings): Unit = {
    busSettings = Some(postedBusSettings)
    val mess = SPAttributes(
      "busSettings" -> busSettings
    )
    sendResponseViaSSE(mess)
  }

  def connectToBus(): Option[SPErrorString] = {
    val result = for {
      b <- getBusSettings.right
    } yield
      ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${b.host}:${b.port}")
    maybeError(result, "Failed to connect to the bus.")
  }

  def disconnectFromBus(): Option[SPErrorString] = {
    val result = for {
      b <- getBus.right
    } yield {
      b ! CloseConnection
      bus = None
      notifyConnectionStatus()
    }
    maybeError(result, "Failed to disconnect from the bus.")
  }

  def startLiveWatch(postedWorkCellId: String) = {
    liveWorkCells = postedWorkCellId :: liveWorkCells
    val mess = SPAttributes(
      "addedLiveWatch" -> SPAttributes(
        "workCellId" -> postedWorkCellId
      )
    )
    sendResponseViaSSE(mess)
  }

  def stopLiveWatch(postedWorkCellId: String) = {
    liveWorkCells = liveWorkCells.filter(w => w != postedWorkCellId)
    val mess = SPAttributes(
      "removedLiveWatch" -> SPAttributes(
        "workCellId" -> postedWorkCellId
      )
    )
    sendResponseViaSSE(mess)
  }

  def getServiceState = {
    val mess = SPAttributes(
      "busSettings" -> busSettings,
      "busConnected" -> bus.isDefined,
      "connectionInterrupted" -> isInterrupted,
      "liveWorkCells" -> liveWorkCells
    )
    Some(toResponse(mess))
  }

  def searchCycles(cycleId: String, timeSpan: TimeSpan, workCellId: String) = {
    val mess = SPAttributes(
      "robotCycleSearchQuery" -> SPAttributes(
        "cycleId" -> cycleId,
        "timeSpan" -> timeSpan,
        "workCellId" -> workCellId
      )
    )
    notifyIfError(sendToBus(mess), "Failed to search for cycles.")

    // TEMPORARY RETURN OF A RESULT UNTIL A SERVICE FOR SEARCHING CYCLES IS READY

    val responseMess = SPAttributes(
      "robotCycleSearchResult" -> SPAttributes(
        "workCellId" -> "117956",
        "foundCycles" -> List(
          SPAttributes(
            "id" -> ID.newID,
            "from" -> DateTime.now.minusHours(6),
            "to"  -> DateTime.now.minusHours(6).plusMinutes(1).plusSeconds(45),
            "activities" -> SPAttributes(
              "R1" -> SPAttributes(),
              "R2" -> SPAttributes(
                "routines" -> List(
                  SPAttributes(
                    "name" -> "PickUpRearLeft",
                    "from" -> DateTime.now.minusHours(6).plusSeconds(10),
                    "to" -> DateTime.now.minusHours(6).plusSeconds(20)
                  ),
                  SPAttributes(
                    "name" -> "MoveToBody",
                    "from" -> DateTime.now.minusHours(6).plusSeconds(25),
                    "to" -> DateTime.now.minusHours(6).plusSeconds(40)
                  )
                )
              )
            )
          )
        )
      )
    )

    eventHandler ! toResponse(responseMess)
  }

  def requestAvailableWorkCells() = {
    val mess = SPAttributes("abbRobotCommand" -> "getAvailableWorkCells")
    notifyIfError(sendToBus(mess), "Failed to request available work cells.")

    // TEMPORARY WHEN ON NON-WINDOWS COMPUTER
    val responseMess = SPAttributes(
      "availableWorkCells" -> List(
        SPAttributes(
          "id" -> "117956",
          "description" -> "Mount rear doors",
          "robots" -> List(
            SPAttributes(
              "id" -> "R1",
              "description" -> "Picks up rear left door and puts it into place"
            ),
            SPAttributes(
              "id" -> "R2",
              "description" -> "Fixates rear left door"
            )
          )
        )
      )
    )
    eventHandler ! toResponse(responseMess)
  }

  def connectionEstablished(request: ConnectionRequest, busConnection: ActorRef) = {
    println("Bus connection established: " + request)
    val setTopic = for {
      s <- getBusSettings.right
    } yield {
      bus = Some(busConnection)
      busConnection ! ConsumeFromTopic(s.topic)
      notifyConnectionStatus()
    }
    notifyIfError(addErrorDescription(setTopic, "Failed to establish a connection."))
  }

  def amqMessage(body: Any, prop: JMSMessageProperties, headers: Map[String, Any]): Unit = {
    import org.json4s._
    import org.json4s.native.JsonMethods._

    val json = body.toString
    val jObject = parse(json)

    val isCycleSearchResult = jObject.has("cycleSearchResult") && jObject.has("workCellId")
    val isCycle = jObject.has("cycle") && (jObject \ "cycle").has("events")
    val isRobotEvent = jObject.has("robotEvent") &&
      liveWorkCells.contains((jObject \ "robotEvent" \ "workCellId").to[String].get)
    val isAvailableWorkCells = jObject.has("availableWorkCells")

    if (isCycle || isCycleSearchResult || isRobotEvent || isAvailableWorkCells) {
      val spAttributes = SPAttributes.fromJson(json).get
      sendResponseViaSSE(spAttributes)
    }
  }

  def connectionInterrupted(ca: ActorRef, x: Boolean) = {
    isInterrupted = true
    notifyConnectionStatus()
  }

  def connectionReestablished(): Unit = {
    isInterrupted = false
    notifyConnectionStatus()
  }

  // --- Service building blocks --- //

  def getBus: Either[SPErrorString, ActorRef] =
    setErrorIfNone(bus, "You are not connected to any bus.")

  def getBusSettings: Either[SPErrorString, BusSettings] =
    setErrorIfNone(busSettings, "Bus settings not complete.")

  def sendToBus(mess: SPAttributes) = {
    val result = for {
      b <- getBus.right
      s <- getBusSettings.right
    } yield {
      //println(s"sending: ${mess.toJson}")
      b ! SendMessage(Topic(s.topic), AMQMessage(mess.toJson))
    }
    addErrorDescription(result, "Failed to send message to bus.")
  }

  // --- Service specific helper functions --- //

  def notifyConnectionStatus(): Unit = {
    val mess = SPAttributes(
      "busConnected" -> bus.isDefined,
      "isInterrupted" -> isInterrupted
    )
    eventHandler ! toResponse(mess)
  }

  override def postStop() = {
    disconnectFromBus()
  }

  // --- Generic helper functions --- //

  def setErrorIfNone[T](option: Option[T], error: String): Either[SPErrorString, T] = option match {
      case Some(o) => Right(o)
      case None    => Left(SPErrorString(error))
    }

  def addErrorDescription[T](result: Either[SPErrorString, T], generalErrorDescription: String): Either[SPErrorString, T] = result match {
      case Left(e)    => Left(SPErrorString(generalErrorDescription + " Reason: " + result.left.get.error))
      case r@Right(_) => r
    }

  def maybeError[T](either: Either[SPErrorString, T]): Option[SPErrorString] = either match {
      case Left(e)  => Some(e)
      case Right(_) => None
    }

  def maybeError[T](either: Either[SPErrorString, T], error: String): Option[SPErrorString] =
    maybeError(addErrorDescription(either, error))

  def toResponse(message: SPAttributes): Response = {
    val mess = SPAttributes("silent" -> JBool(true) :: message.obj)
    Response(List(), mess, spServiceName, spServiceID)
  }

  def toServiceError(error: SPError): ServiceError =
    ServiceError(spServiceName, spServiceID, error)

  def okResponse(): Response =
    toResponse(SPAttributes())

  def sendResponseViaSSE(message: SPAttributes): Unit =
    eventHandler ! toResponse(message)

  def notifyIfError[T](result: Either[SPErrorString, T]): Unit =
    if (result.isLeft) {
      println("Notifying error " + result.left.get)
      eventHandler ! toServiceError(result.left.get)
    }


  def notifyIfError[T](result: Either[SPErrorString, T], error: String): Unit =
      notifyIfError(addErrorDescription(result, error))

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