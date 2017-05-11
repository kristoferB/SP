package sp.elvisdatahandler

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import scala.util.matching.Regex
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.collection.mutable.ListBuffer

import java.util.Date
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.chrono.ChronoLocalDate

import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization._
import org.json4s._
import org.json4s.JsonDSL._

import sp.elvisdatahandler.{API_PatientEvent => api}
import sp.elvisdatahandler.{API_Data => dataApi}
import sp.elvisdatahandler.{API_Patient => patientApi}


/**
*  This is the actor (the service) that listens for messages on the bus
*/
class ElvisDataHandlerDevice extends Actor with ActorLogging {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization


  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("elvis-diff", self)

  var state: Map[Int, dataApi.EricaPatient] = Map()
  var guiState: Map[String, patientApi.Patient] = Map()

  /**
  Receives incoming messages on the AKKA-bus
  */
  def receive = {
    case "tick" => //updateAllTimeDiffs()
    case x: String => handleRequests(x)
  }

  /**
  Handles the received message and sends it further
  */
  def handleRequests(x: String): Unit = {
    val mess = SPMessage.fromJson(x)
    matchRequests(mess)
  }

 def matchRequests(mess: Try[SPMessage]) = {
  ElvisDataHandlerComm.extractElvisEvent(mess) map { case (h, b) =>
    b match {
      case api.ElvisData(s) => {
          state = Map() // clear state before assembling new that was received
          var visited: Map[Int, Boolean] = Map()
          s.foreach{ e => {
            if (!visited.contains(e.CareContactId)) {
              if (s.filter(_.CareContactId == e.CareContactId).filter(_.Category == "RemovedPatient").isEmpty) {
                state += e.CareContactId -> constructPatient(s.filter(_.CareContactId == e.CareContactId))
              } else {
                state -= e.CareContactId
              }
              visited += e.CareContactId -> true
              }
            }
          }
          createVisualizablePatients(state)
      }
        //handlePatient(handleMessage(s)) <-- according to old structure
    }
  }
}

def createVisualizablePatients(patients: Map[Int, dataApi.EricaPatient]) {
  guiState = Map() // clear before assembling new
  patients.foreach{ p =>
    guiState += (p._2.CareContactId.toString -> patientApi.Patient(
      p._2.CareContactId.toString,
      patientApi.Priority(p._2.Priority, ""),
      patientApi.Attended(p._2.IsAttended, "", ""),
      patientApi.Location(decodeLocation(p._2.Location), ""),
      patientApi.Team(getTeam(p._2.Clinic, p._2.Location, p._2.ReasonForVisit), p._2.Clinic, p._2.ReasonForVisit, ""),
      patientApi.Examination(p._2.OnExamination, ""),
      patientApi.LatestEvent(p._2.LatestEvent, p._2.LatestEventTimeDiff, false, ""),
      patientApi.Plan(p._2.HasPlan, ""),
      patientApi.ArrivalTime(getArrivalFormat(p._2.VisitRegistrationTime), ""),
      patientApi.Debugging(p._2.Clinic, p._2.ReasonForVisit, p._2.Location),
      patientApi.Finished(p._2.IsFinished, p._2.IsFinished, "")
    ))
  }
  publishOnAkka(SPHeader(from = "elvisDataHandler"), api.State(guiState))
}

def constructPatient(events: List[dataApi.EricaEvent]): dataApi.EricaPatient = {
  val (latestEvent, timeDiff) = getLatestEvent(events.filter( e => (e.Category == "T" || e.Category == "U" || e.Category == "Q")))
  dataApi.EricaPatient(
        events(0).CareContactId,
        getValue(events.filter(_.Category == "DepartmentCommentUpdate")),
        getValue(events.filter(_.Category == "LocationUpdate")),
        getValue(events.filter(_.Category == "ReasonForVisitUpdate")),
        getValue(events.filter(_.Category == "TeamUpdate")),
        getPriority(events.filter(_.Category == "P")),
        latestEvent,
        timeDiff,
        getIsAttended(events.filter(_.Category == "T")),
        getNeedsAttention(timeDiff, getPriority(events.filter(_.Category == "P"))),
        getOnExamination(events.filter(_.Category == "T")),
        getHasPlan(events.filter(_.Category == "T")),
        getIsFinished(events.filter(_.Category == "T")),
        events(0).VisitId,
        getValue(events.filter(_.Category == "VisitRegistrationTimeUpdate"))
      )
}

def publishOnAkka(header: SPHeader, body: api.Event) {
  val toSend = ElvisDataHandlerComm.makeMess(header, body)
  toSend match {
    case Success(v) => {
      mediator ! Publish("state-event", v)
    }
    case Failure(e) =>
      println("Failed")
  }
}

/**
* Returns "hh:mm (day)"-format of argument date string in format yyyy-MM-dd'T'HH:mm:ssZ.
*/
def getArrivalFormat(startTimeString: String): String = {
  var formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
  val startTime = formatter.parse(startTimeString.replaceAll("Z$", "+0000"))
  val timeFormat = new SimpleDateFormat("HH:mm")
  val timeString = timeFormat.format(startTime)
  val diff = getTimeDiff(startTimeString)
  val days = (diff / (1000*60*60*24))
  var dayString = ""
  days match {
    case 0 => dayString = ""
    case 1 => dayString = "(igår)"
    case (n: Long) => dayString = "(+" + n + " d.)"
  }
  return timeString + " " + dayString
}

/**
Returns the time difference (in milliseconds) between a given start time and now.
Argument startTimeString must be received in date-time-format: yyyy-MM-ddTHH:mm:ssZ
*/
def getTimeDiff(startTimeString: String): Long = {
  if (startTimeString != "") {
    var formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    val startTime = formatter.parse(startTimeString.replaceAll("Z$", "+0000"))
    val now: Long = System.currentTimeMillis
    return Math.abs(now - startTime.getTime()) // returns diff in millisec
  }
  return 0
}

def getTeam(clinic: String, location: String, reasonForVisit: String): String = {
  reasonForVisit match {
    case "AKP" => "stream"
    case "ALL" | "TRAU" => "process"
    case "B" | "MEP" => {
        if (location != "") {
          location.charAt(0) match {
            case 'G' => "medicin gul"
            case 'B' => "medicin blå"
            case 'P' => "process"
            case _ => {
              clinic match {
                case "NAKME" => "medicin"
                case "NAKKI" => "kirurgi"
                case "NAKOR" => "ortopedi"
                case "NAKOR" | "NAKBA" | "NAKÖN" => "jour"
                case "NAKM" => "NAKM"
                case _ => "no-match"
              }
            }
          }
        } else {
          clinic match {
            case "NAKME" => "medicin"
            case "NAKKI" => "kirurgi"
            case "NAKOR" => "ortopedi"
            case "NAKOR" | "NAKBA" | "NAKÖN" => "jour"
            case "NAKM" => "NAKM"
            case _ => "no-match"
          }
        }
    }
    case _ => "no-match"
  }
}

/**
* Checks if patient needs attention, according to some specific guidelines.
*/
def getNeedsAttention(timeDiff: Long, priority: String): Boolean = {
  priority match {
    case "Blue" | "Green" | "Yellow" => if (timeDiff > 3600000) true else false
    case "Orange" => if (timeDiff > 1200000) true else false
    case "Red" => true
    case _ => false
  }
}

def getIsFinished(events: List[dataApi.EricaEvent]): Boolean = {
  events.foreach{ e => if (e.Title == "Klar") { return true } }
  return false
}

def getHasPlan(events: List[dataApi.EricaEvent]): Boolean = {
  events.foreach{ e => if (e.Title == "Plan") { return true } }
  return false
}

def getOnExamination(events: List[dataApi.EricaEvent]): Boolean = {
  events.foreach{ e => if (e.Title == "Rö/klin" && e.End == "0001-01-02T23:00:00Z") { return true } }
  return false
}

def getIsAttended(events: List[dataApi.EricaEvent]): Boolean = {
  events.foreach{ e => if (e.Title == "Läkare") { return true } }
  return false
}

def getLatestEvent(events: List[dataApi.EricaEvent]): (String, Long) = {
  var formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
  val tmp = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").format(new Date())
  var startOfLatestEvent = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").parse(tmp)
  var latestEvent = ""
  var latestEventTimeDiffString = ""

  events.foreach{ e =>
    val startOfEvent = formatter.parse(e.Start.replaceAll("Z$", "+0000"))
    if (startOfEvent.after(startOfLatestEvent)) {
      latestEvent = e.Title
      latestEventTimeDiffString = e.Start
      startOfLatestEvent = startOfEvent
    }
  }
  return (latestEvent, getTimeDiff(latestEventTimeDiffString))
}

def getPriority(events: List[dataApi.EricaEvent]): String = {
  var formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
  val tmp = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").format(new Date())
  var startOfLatestEvent = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").parse(tmp)
  var latestPrioEvent = "Otriagerad"

  events.foreach{ e =>
    if (isValidTriageColor(e.Title)) {
      val startOfEvent = formatter.parse(e.Start.replaceAll("Z$", "+0000"))
      if (startOfEvent.after(startOfLatestEvent)) {
        latestPrioEvent = e.Title
        startOfLatestEvent = startOfEvent
      }
    }
  }
  return translate(latestPrioEvent)
}

def translate(str: String): String = {
  str match {
    case "Otriagerad" => "NotTriaged"
    case "Blå" => "Blue"
    case "Grön" => "Green"
    case "Gul" => "Yellow"
    case "Röd" => "Red"
    case _ => str
  }
}

/**
Filters out a room nr to present from an apiPatient.Location.
*/
def decodeLocation(location: String): String = {
  if (location matches "[GgBbPp]([Tt]|[Ii])[1,4]") {
    location.replaceAll("[^0-9]","")
  } else if (location matches "[BbGgPp].{2}") {
    location.replaceAll("[^0-9]","")
  } else location
}

/**
Checks if string is valid triage color.
*/
def isValidTriageColor(string: String): Boolean = {
  if (string == "Blå" || string == "Grön" || string == "Gul" || string == "Orange" || string == "Röd") {
    return true
  }
  return false
}

def getValue(events: List[dataApi.EricaEvent]): String = {
  events.foreach{ e => return e.Value}
  return ""
}

val info = SPAttributes(
   "service" -> api.attributes.service,
   "group" -> "runtime"
 )

  // A "ticker" that sends a "tick" string to self every 1 minute
  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(0 seconds, 1 minutes, self, "tick")

}

object ElvisDataHandlerDevice {
  def props = Props(classOf[ElvisDataHandlerDevice])
}
