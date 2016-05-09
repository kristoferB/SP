package sp.robotCycleAnalysis

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
import org.json4s.JsonAST.JNothing

import scala.collection.mutable.ListBuffer

object RobotCycleAnalysis extends SPService {
  object Commands extends Enumeration {
    type Command = Value
    val SetupBus = Value("setupBus")
    val ConnectToBus = Value("connectToBus")
    val DisconnectFromBus = Value("disconnectFromBus")
    val StartLiveWatch = Value("startLiveWatch")
    val StopLiveWatch = Value("stopLiveWatch")
    val RequestCycles = Value("requestCycles")
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
    "workCell" -> SPAttributes(
      "name"        -> KeyDefinition("String", List(), Some("not set")),
      "description" -> KeyDefinition("String", List(), Some("not set"))
    ),
    "timeSpan" -> SPAttributes(
      "start" -> KeyDefinition("String", List(), Some("not set")),
      "stop" -> KeyDefinition("String", List(), Some("not set"))
    ),
    "command" -> KeyDefinition("String", Commands.stringList, Some("getServiceState")),
    "cycle"   -> SPAttributes(
      "id"      -> KeyDefinition("Option[String]", List(), None)
    ),
    "robotCyclesRequest" -> SPAttributes(
      "cycleIds" -> KeyDefinition("Option[List[String]]", List(), None)
    )
  )

  case class TransformValues(busSettings: TransformValue[BusSettings],
                             workCell: TransformValue[WorkCell],
                             timeSpan: TransformValue[TimeSpan],
                             command: TransformValue[String],
                             cycle: TransformValue[Cycle],
                             robotCyclesRequest: TransformValue[RobotCyclesRequest])

  val transformValues = TransformValues(
    TransformValue("busSettings", _.getAs[BusSettings]("busSettings")),
    TransformValue("workCell", _.getAs[WorkCell]("workCell")),
    TransformValue("timeSpan", _.getAs[TimeSpan]("timeSpan")),
    TransformValue("command", _.getAs[String]("command")),
    TransformValue("cycle", _.getAs[Cycle]("cycle")),
    TransformValue("robotCyclesRequest", _.getAs[RobotCyclesRequest]("robotCyclesRequest"))
  )

  override val transformation = transformToList(transformValues.productIterator.toList)
  def props(eventHandler: ActorRef) = Props(classOf[RobotCycleAnalysis], eventHandler)
}

case class RobotCyclesRequest(cycleIds: Option[List[String]])
case class BusSettings(host: String, port: Int, topic: String)
case class Robot(name: String)
case class WorkCell(name: String, description: String)
case class TimeSpan(start: String, stop: String)
case class Cycle(id: Option[ID], start: Option[String], stop: Option[String], workCell: Option[WorkCell],
                 events: Option[Map[String, List[CycleEvent]]])
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

  var liveWorkCells: List[WorkCell] = List.empty

  def receive = {
    case RegisterService(s, _, _,_) => println(s"Service $s is registered")
    case request @ Request(service, attr, ids, reqID) =>
      val theSender = sender()
      implicit val requestAndSender = RequestNReply(request, theSender)
      import RobotCycleAnalysis._
      import RobotCycleAnalysis.Commands._

      val postedBusSettings = transform(transformValues.busSettings)
      val postedCycle = transform(transformValues.cycle)
      val postedRobotCyclesRequest = transform(transformValues.robotCyclesRequest)
      val postedTimeSpan = transform(transformValues.timeSpan)
      val postedWorkCell = transform(transformValues.workCell)

      val commandString = transform(transformValues.command)
      val command = Commands.withName(commandString)
      var answer: Option[SPEvent] = None

      println("Got command: " + command)

      command match {
        case ConnectToBus => answer = connectToBus()
        case DisconnectFromBus => answer = disconnectFromBus()
        case GetServiceState => answer = getServiceState
        case RequestAvailableWorkCells => requestAvailableWorkCells()
        case RequestCycles => requestCycles(postedRobotCyclesRequest)
        case SearchCycles => searchCycles(postedCycle, postedTimeSpan, postedWorkCell)
        case SetupBus => setupBus(postedBusSettings)
        case StartLiveWatch => startLiveWatch(postedWorkCell)
        case StopLiveWatch => stopLiveWatch(postedWorkCell)
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

  def startLiveWatch(postedWorkCell: WorkCell) = {
    liveWorkCells = postedWorkCell :: liveWorkCells
    val mess = SPAttributes(
      "addedLiveWatch" -> postedWorkCell
    )
    sendResponseViaSSE(mess)
  }

  def stopLiveWatch(postedWorkCell: WorkCell) = {
    liveWorkCells = liveWorkCells.filter(w => w.name != postedWorkCell.name)
    val mess = SPAttributes(
      "removedLiveWatch" -> postedWorkCell
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

  def searchCycles(cycle: Cycle, timeSpan: TimeSpan, workCell: WorkCell) = {
    val mess = SPAttributes(
      "robotCycleSearchQuery" -> SPAttributes(
        "cycle" -> cycle,
        "timeSpan" -> timeSpan,
        "workCell" -> workCell
      )
    )
    notifyIfError(sendToBus(mess), "Failed to search for cycles.")

    // TEMPORARY RETURN OF A RESULT UNTIL A SERVICE FOR SEARCHING CYCLES IS READY
    var cycleSearchResult = new ListBuffer[SPAttributes]()
    var time = DateTime.now.minusHours(6)
    for (_ <- 1 to 40) {
      val cycle = SPAttributes(
        "id" -> ID.newID,
        "start" -> time,
        "stop" -> time.plus(Period.minutes(2))
      )
      time = time.plus(Period.minutes(3))
      cycleSearchResult += cycle
    }
    val responseMess = SPAttributes(
      "cycleSearchResult" -> cycleSearchResult,
      "workCellName" -> "117956"
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
          "name" -> "117956",
          "description" -> "Mount rear doors",
          "robots" -> List(
            SPAttributes(
              "name" -> "R1",
              "description" -> "Picks up rear left door and puts it into place",
              "routines" -> List(
                "27" -> SPAttributes(
                  "name" -> "MoveP12P43",
                  "description" -> "Picking up rear left door"
                )
              )
            ),
            SPAttributes(
              "name" -> "R2",
              "description" -> "Fixates rear left door",
              "routines" -> List()
            )
          )
        )
      )
    )
    eventHandler ! toResponse(responseMess)
  }

  def requestCycles(robotCyclesRequest: RobotCyclesRequest) = {
    val mess = for {
      ids <- setErrorIfNone(robotCyclesRequest.cycleIds, "The request misses a list of cycle ids.").right
    } yield SPAttributes(
      "robotCyclesRequest" -> SPAttributes(
        "cycleIds" -> ids
      )
    )

    /*val mess = SPAttributes(
      "robotCyclesRequest" -> SPAttributes(
        "cycleIds" -> robotCyclesRequest.cycleIds
      )
    )*/

    val result = for {
      m <- mess.right
      s <- sendToBus(m).right
    } yield Unit

    notifyIfError(result, "Failed to request cycles.")

    // TEMPORARY RESPONSE UNTIL A SERVICE FOR REQUESTING CYCLES IS READY
    val responseMess = SPAttributes(
      "robotCyclesResponse" -> SPAttributes(
        "workCellName" -> "117956",
        "foundCycles" -> List(
          SPAttributes(
            "id" -> ID.newID,
            "start" -> DateTime.now.minusHours(6),
            "stop"  -> DateTime.now.minusHours(6).plusMinutes(2),
            "events" -> SPAttributes(
              "R1" -> SPAttributes(),
              "R2" -> SPAttributes(
                ID.newID.toString -> SPAttributes(
                  "routineNumber" -> 27,
                  "start" -> DateTime.now.minusHours(6).plusSeconds(20),
                  "stop" -> DateTime.now.minusHours(6).plusSeconds(30)
                )
              )
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

    val isCycleSearchResult = jObject.has("cycleSearchResult") && jObject.has("workCell")
    val isCycle = jObject.has("cycle") && (jObject \ "cycle").has("events")
    val isCycleEvent = jObject.has("cycleEvent") &&
      liveWorkCells.exists(w => w.name == (jObject \ "cycleEvent" \ "workCell" \ "name").to[String].get)
    val isAvailableWorkCells = jObject.has("availableWorkCells")

    if (isCycle || isCycleSearchResult || isCycleEvent || isAvailableWorkCells) {
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
      println(s"sending: ${mess.toJson}")
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

  def toResponse(message: SPAttributes): Response =
    Response(List(), message, spServiceName, spServiceID)

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