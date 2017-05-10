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
  mediator ! Subscribe("elvis-data-topic", self)
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
          var visited: Map[Int, Boolean] = Map()
          s.foreach{ e => {
            if (!visited.contains(e.CareContactId)) {
              //println("Events for ccid: " + e.CareContactId)
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
  patients.foreach{ p =>
    guiState += (p._2.CareContactId.toString -> patientApi.Patient(
      p._2.CareContactId.toString,
      patientApi.Priority(p._2.Priority, ""),
      patientApi.Attended(p._2.IsAttended, "", ""),
      patientApi.Location(p._2.Location, ""),
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

def publishOnAkka(header: SPHeader, body: api.Event) {
  val toSend = ElvisDataHandlerComm.makeMess(header, body)
  toSend match {
    case Success(v) => {
      mediator ! Publish("patient-event-topic", v)
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

 /**
 * Acts according to which kind of patient was received (new, diff or removed).
 */
 /**
 def handlePatient(patient: api.PatientEvent) {
   patient match {
     case api.NewPatient(careContactId, patientData, events) => {
       val patientProperties = extractNewPatientProperties(api.NewPatient(careContactId, patientData, events))
       if (!patientProperties.isEmpty) {
         println("NEW CCID: " + careContactId + " -> " + patientProperties)
         for (patientProperty <- patientProperties) {
           updateState(careContactId, patientProperty)
         }
         publishOnAkka(SPHeader(from = "elvisDataHandler"), api.State(state))
       }
     }
     case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
       val patientProperties = extractDiffPatientProperties(api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents))
       if (!patientProperties.isEmpty) {
         println("DIFF CCID: " + careContactId + " -> " + patientProperties)
         for (patientProperty <- patientProperties) {
           updateState(careContactId, patientProperty)
         }
         publishOnAkka(SPHeader(from = "elvisDataHandler"), api.State(state))
       }
     }
     case api.RemovedPatient(careContactId, timestamp) => {
       println("REMOVED CCID: " + careContactId)
       updateState(careContactId, apiPatient.Removed(timestamp))
       publishOnAkka(SPHeader(from = "elvisDataHandler"), api.State(state))
     }
     case _ => println("Not expected")
   }
 }*/

 /**
 * Updates all time differences + 1 min.
 *//**
 def updateAllTimeDiffs() {
   state.foreach{ p =>
     if (p._2.latestEvent.timeDiff != -1) {
       updateState(p._1, apiPatient.LatestEvent(p._2.latestEvent.latestEvent, p._2.latestEvent.timeDiff + 60000, p._2.latestEvent.needsAttention, p._2.latestEvent.timestamp)) // 60 000 ms is one minute
     }
   }
 }*/

 /**
 * Updates the current state based on what patient property is received.
 */
  //---------------------------------------HÄR?
  /**
 def updateState(careContactId: String, prop: dataApi.EricaPatientProperty) {
   if (state.keys.exists(_ == careContactId)) {
     if (prop.isInstanceOf[apiPatient.Removed]) {
       state -= careContactId
     } else {
       state += (careContactId -> updateExistingPatient(state, careContactId, prop))
     }
   } else {
     state += (careContactId -> updateNewPatient(careContactId, prop))
   }
 }*/

 /**
 * Constructs a new patient object.
 */
 /**
 def updateNewPatient(ccid: String, prop: dataApi.EricaPatientProperty): dataApi.EricaPatient = {
   prop match {
     case apiPatient.Priority(color, timestamp) => dataApi.EricaPatient(ccid, apiPatient.Priority(color, timestamp), apiPatient.Attended(false, "NA", ""), apiPatient.Location("", ""), apiPatient.Team("", "", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.Plan(false, ""), apiPatient.ArrivalTime("", ""), apiPatient.Debugging("","",""), apiPatient.Finished(false, false, ""))
     case apiPatient.Attended(attended, doctorId, timestamp) => dataApi.EricaPatient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(attended, doctorId, timestamp), apiPatient.Location("", ""), apiPatient.Team("", "", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.Plan(false, ""), apiPatient.ArrivalTime("", ""), apiPatient.Debugging("","",""), apiPatient.Finished(false, false, ""))
     case apiPatient.Location(roomNr, timestamp) => dataApi.EricaPatient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "NA", ""), apiPatient.Location(roomNr, timestamp), apiPatient.Team("", "", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.Plan(false, ""), apiPatient.ArrivalTime("", ""), apiPatient.Debugging("","",""), apiPatient.Finished(false, false, ""))
     case apiPatient.Team(team, clinic, reasonForVisit, timestamp) => dataApi.EricaPatient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "NA", ""), apiPatient.Location("", ""), apiPatient.Team(team, clinic, reasonForVisit, timestamp), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.Plan(false, ""), apiPatient.ArrivalTime("", ""), apiPatient.Debugging("","",""), apiPatient.Finished(false, false, ""))
     case apiPatient.Examination(isOnExam, timestamp) => dataApi.EricaPatient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "NA", ""), apiPatient.Location("", ""), apiPatient.Team("", "", "", ""), apiPatient.Examination(isOnExam, timestamp), apiPatient.LatestEvent("", -1, false, ""), apiPatient.Plan(false, ""), apiPatient.ArrivalTime("", ""), apiPatient.Debugging("","",""), apiPatient.Finished(false, false, ""))
     case apiPatient.LatestEvent(latestEvent, timeDiff, needsAttention, timestamp) => dataApi.EricaPatient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "NA", ""), apiPatient.Location("", ""), apiPatient.Team("", "", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent(latestEvent, timeDiff, needsAttention, timestamp), apiPatient.Plan(false, ""), apiPatient.ArrivalTime("", ""), apiPatient.Debugging("","",""), apiPatient.Finished(false, false, ""))
     case apiPatient.Plan(hasPlan, timestamp) => dataApi.EricaPatient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "NA", ""), apiPatient.Location("", ""), apiPatient.Team("", "", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.Plan(hasPlan, timestamp), apiPatient.ArrivalTime("", ""), apiPatient.Debugging("","",""), apiPatient.Finished(false, false, ""))
     case apiPatient.ArrivalTime(timeDiff, timestamp) => dataApi.EricaPatient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "NA", ""), apiPatient.Location("", ""), apiPatient.Team("", "", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.Plan(false, ""), apiPatient.ArrivalTime(timeDiff, timestamp), apiPatient.Debugging("","",""), apiPatient.Finished(false, false, ""))
     case apiPatient.Debugging(clinic, reasonForVisit, location) => dataApi.EricaPatient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "NA", ""), apiPatient.Location("", ""), apiPatient.Team("", "", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.Plan(false, ""), apiPatient.ArrivalTime("", ""), apiPatient.Debugging(clinic, reasonForVisit, location), apiPatient.Finished(false, false, ""))
     case apiPatient.Finished(finished, finishedStillPresent, timestamp) => dataApi.EricaPatient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "NA", ""), apiPatient.Location("", ""), apiPatient.Team("", "", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.Plan(false, ""), apiPatient.ArrivalTime("", ""), apiPatient.Debugging("","",""), apiPatient.Finished(finished, finishedStillPresent, timestamp))
     case _ => dataApi.EricaPatient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "NA", ""), apiPatient.Location("", ""), apiPatient.Team("", "", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.Plan(false, ""), apiPatient.ArrivalTime("", ""), apiPatient.Debugging("","",""), apiPatient.Finished(false, false, ""))
   }
 }*/

 /**
 * Constructs an updates patient object.
 *//**
 def updateExistingPatient(s: Map[String, dataApi.EricaPatient], ccid: String, prop: dataApi.EricaPatientProperty): dataApi.EricaPatient = {
   prop match {
     case apiPatient.Priority(color, timestamp) => dataApi.EricaPatient(ccid, apiPatient.Priority(color, timestamp), s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).plan, s(ccid).arrivalTime, s(ccid).debugging, s(ccid).finished)
     case apiPatient.Attended(attended, doctorId, timestamp) => dataApi.EricaPatient(ccid, s(ccid).priority, apiPatient.Attended(attended, doctorId, timestamp), s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).plan, s(ccid).arrivalTime, s(ccid).debugging, s(ccid).finished)
     case apiPatient.Location(roomNr, timestamp) => dataApi.EricaPatient(ccid, s(ccid).priority, s(ccid).attended, apiPatient.Location(roomNr, timestamp), s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).plan, s(ccid).arrivalTime, s(ccid).debugging, s(ccid).finished)
     case apiPatient.Team(team, clinic, reasonForVisit, timestamp) => dataApi.EricaPatient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, apiPatient.Team(team, clinic, reasonForVisit, timestamp), s(ccid).examination, s(ccid).latestEvent, s(ccid).plan, s(ccid).arrivalTime, s(ccid).debugging, s(ccid).finished)
     case apiPatient.Examination(isOnExam, timestamp) => dataApi.EricaPatient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, apiPatient.Examination(isOnExam, timestamp), s(ccid).latestEvent, s(ccid).plan, s(ccid).arrivalTime, s(ccid).debugging, s(ccid).finished)
     case apiPatient.LatestEvent(latestEvent, timeDiff, needsAttention, timestamp) => dataApi.EricaPatient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, apiPatient.LatestEvent(latestEvent, timeDiff, needsAttention, timestamp), s(ccid).plan, s(ccid).arrivalTime, s(ccid).debugging, s(ccid).finished)
     case apiPatient.Plan(hasPlan, timestamp) => dataApi.EricaPatient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, apiPatient.Plan(hasPlan, timestamp), s(ccid).arrivalTime, s(ccid).debugging, s(ccid).finished)
     case apiPatient.ArrivalTime(timeDiff, timestamp) => dataApi.EricaPatient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).plan, apiPatient.ArrivalTime(timeDiff, timestamp), s(ccid).debugging, s(ccid).finished)
     case apiPatient.Debugging(clinic, reasonForVisit, location) => dataApi.EricaPatient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).plan, s(ccid).arrivalTime, apiPatient.Debugging(clinic, reasonForVisit, location), s(ccid).finished)
     case apiPatient.Finished(finished, finishedStillPresent, timestamp) => dataApi.EricaPatient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).plan, s(ccid).arrivalTime, s(ccid).debugging, apiPatient.Finished(finished, finishedStillPresent, timestamp))
     case _ => dataApi.EricaPatient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).plan, s(ccid).arrivalTime, s(ccid).debugging, s(ccid).finished)
   }
 }*/

 /**
 * Marks patient as removed.
 *//**
 def updateRemovedPatient(s: Map[String, dataApi.EricaPatient], ccid: String, prop: dataApi.EricaPatientProperty): dataApi.EricaPatient = {
   prop match {
     case apiPatient.Finished(finished, finishedStillPresent, timestamp) => dataApi.EricaPatient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).plan, s(ccid).arrivalTime, s(ccid).debugging, apiPatient.Finished(finished, finishedStillPresent, timestamp))
     case _ => dataApi.EricaPatient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).plan, s(ccid).arrivalTime, s(ccid).debugging, s(ccid).finished)
   }
 }*/

 /**
 Takes a NewPatient and returns PatientProperties based on patient data and events.
 */
/**
 def extractNewPatientProperties(patient: api.NewPatient): List[dataApi.EricaPatientProperty] = {
   return filterNewPatientProperties(patient, getNewPatientProperties(patient))
 }*/

 /**
 Takes a DiffPatient and returns PatientProperties based on updates and new events.
 *//**
 def extractDiffPatientProperties(patient: api.DiffPatient): List[dataApi.EricaPatientProperty] = {
   return filterDiffPatientProperties(patient, getDiffPatientProperties(patient))
 }
*/
 /**
 Takes a NewPatient and extracts PatientProperties based on patient data and events.
 *//**
 def getNewPatientProperties(patient: api.NewPatient): List[dataApi.EricaPatientProperty] = {
   var patientPropertyBuffer = new ListBuffer[dataApi.EricaPatientProperty]()

   // Update properties based on the patient data
   patient.patientData.foreach{ p =>
     p._1 match {
       case "Team" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), p._2, patient.patientData("ReasonForVisit"), patient.patientData("Location"), "NA", "NA")
       case "Location" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updateLocation(patient.careContactId, patient.patientData("timestamp"), p._2)
       case "Priority" => patientPropertyBuffer += updatePriority(patient.careContactId, patient.patientData("timestamp"), p._2)
       case "VisitRegistrationTime" => patientPropertyBuffer += updateArrivalTime(patient.careContactId, p._2)
       case _ => patientPropertyBuffer += apiPatient.Undefined()
     }
   }

   // Update properties based on new events
   patientPropertyBuffer += updateAttended(patient.careContactId, patient.events)
   patientPropertyBuffer += updateLatestEvent(patient.careContactId, patient.events, patient.patientData("Priority"))
   patientPropertyBuffer += updatePlan(patient.careContactId, patient.events)
   patientPropertyBuffer += updateExamination(patient.careContactId, patient.events)
   patient.patientData.foreach{ p => // v--- FOR DEBUGGING ---v
     p._1 match {
       case "Team" => if (!fieldEmpty(p._2)) patientPropertyBuffer += apiPatient.Debugging(p._2, patient.patientData("ReasonForVisit"), patient.patientData("Location"))
       case _ => patientPropertyBuffer += apiPatient.Undefined()
     }
   }// ^--- FOR DEBUGGING ---^
   patientPropertyBuffer += updateFinished(patient.careContactId, patient.events, false)
   return patientPropertyBuffer.toList
 }

 def hasReasonForVisitAndLocation(data: Map[String, String]): Boolean = {
   if (data.contains("ReasonForVisit") && data.contains("Location")) {
     return true
   }
   return false
 }

 def hasLocation(data: Map[String, String]): Boolean = {
   if (data.contains("Location")) {
     return true
   }
   return false
 }*/

 /**
 Takes a DiffPatient and extracts PatientProperties based on updates and events.
 *//**
 def getDiffPatientProperties(patient: api.DiffPatient): List[dataApi.EricaPatientProperty] = {
   var patientPropertyBuffer = new ListBuffer[dataApi.EricaPatientProperty]()
   var teamUpdated: Boolean = false
   var locationUpdated: Boolean = false

   // Update properties based on the patient data
   patient.patientData.foreach{ p =>
     p._1 match {
       case "Team" => {
         if (hasReasonForVisitAndLocation(patient.patientData)) {
           teamUpdated = true
           patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), p._2, patient.patientData("ReasonForVisit"), patient.patientData("Location"), state(patient.careContactId).team.team, state(patient.careContactId).team.reasonForVisit)
         } else if (hasLocation(patient.patientData)) {
           teamUpdated = true
           patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), p._2, "NA", patient.patientData("Location"), state(patient.careContactId).team.team, state(patient.careContactId).team.reasonForVisit)
         } else {
           patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), p._2, "NA", "NA", state(patient.careContactId).team.team, state(patient.careContactId).team.reasonForVisit)
         }
       }
       case "Location" => {
         if (!fieldEmpty(p._2)) {
           patientPropertyBuffer += updateLocation(patient.careContactId, patient.patientData("timestamp"), p._2)
           locationUpdated = true
           if (!teamUpdated) {
             patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), state(patient.careContactId).team.team, state(patient.careContactId).team.reasonForVisit, p._2, "given", "given")
           }
         } else {
           if (state(patient.careContactId).location.roomNr != "") {
             println("Patient " + patient.careContactId + " had location " + state(patient.careContactId).location.roomNr + ", now has diff with empty location field.")
             patientPropertyBuffer += apiPatient.Location(p._2, patient.patientData("timestamp"))
             locationUpdated = true
             if (!teamUpdated) {
               patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), state(patient.careContactId).team.team, state(patient.careContactId).team.reasonForVisit, p._2, "given", "given")
             }
           }
         }
       }
       case "ReasonForVisit" => {
         if (!teamUpdated && !locationUpdated) {
           patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), state(patient.careContactId).team.team, p._2, "NA", "given", "new")
         } else if (locationUpdated && !teamUpdated) {
           patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), state(patient.careContactId).team.team, p._2, patient.patientData("Location"), "given", "new")
         } else if (locationUpdated && teamUpdated) {
           patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), patient.patientData("Team"), p._2, patient.patientData("Location"), "new", "new")
         }
       }
       case _ => patientPropertyBuffer += apiPatient.Undefined()
     }
   }



   // Update properties based on new events
   patient.newEvents.foreach{ e =>
     if (e("Title") == "Läkare") {
       patientPropertyBuffer += apiPatient.Attended(true, e("Value"), e("Start"))
     }
   }
   patientPropertyBuffer += getLatestPrioEvent(patient.careContactId, patient.newEvents)
   patientPropertyBuffer += updateLatestEvent(patient.careContactId, patient.newEvents, state(patient.careContactId).priority.color)
   patientPropertyBuffer += updatePlan(patient.careContactId, patient.newEvents)
   patientPropertyBuffer += updateExamination(patient.careContactId, patient.newEvents)
   patient.patientData.foreach{ p => // v--- FOR DEBUGGING ---v
     p._1 match {
       case "Team" => if (!fieldEmpty(p._2)) patientPropertyBuffer += apiPatient.Debugging(p._2, patient.patientData("ReasonForVisit"), patient.patientData("Location"))
       case _ => patientPropertyBuffer += apiPatient.Undefined()
     }
   }// ^--- FOR DEBUGGING ---^
   patientPropertyBuffer += updateFinished(patient.careContactId, patient.newEvents, state(patient.careContactId).finished.finished)
   return patientPropertyBuffer.toList
 }*/

 /**
 Filters out unwanted patient properties.
 *//**
 def filterNewPatientProperties(patient: api.NewPatient, patientProperties: List[dataApi.EricaPatientProperty]): List[dataApi.EricaPatientProperty] = {
   patientProperties
     .filter(_ != apiPatient.Undefined())
     .filter(_ != apiPatient.LatestEvent("", -1, false, ""))
     .filter(_ != apiPatient.Examination(false, ""))
     .filter(_ != apiPatient.Finished(false, false, ""))
     .filter(_ != apiPatient.Debugging("", "", ""))
     .filter(_ != apiPatient.Attended(false, "NA", ""))
     .filter(_ != apiPatient.Plan(false, ""))
 }*/

 /**
 Filters out unwanted patient properties.
 *//**
 def filterDiffPatientProperties(patient: api.DiffPatient, patientProperties: List[dataApi.EricaPatientProperty]): List[dataApi.EricaPatientProperty] = {
   patientProperties
     .filter(_ != apiPatient.Undefined())
     .filter(_ != apiPatient.LatestEvent("", -1, false, ""))
     .filter(_ != apiPatient.Examination(false, ""))
     .filter(_ != apiPatient.Finished(false, false, ""))
     .filter(_ != apiPatient.Debugging("", "", ""))
     .filter(_ != apiPatient.Attended(false, "NA", ""))
     .filter(_ != apiPatient.Plan(false, ""))
 }*/

 /**
 Identifies the latest event considering triage and returns corresponding apiPatient.PriorityEvent.
 OBS: revidera
 *//**
 def getLatestPrioEvent(careContactId: String, events: List[Map[String, String]]): dataApi.EricaPatientProperty = {
   var prio = "NA"
   var latestEventString = "NA"
   var formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
   val tmp = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").format(new Date())
   var startOfLatestEvent = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").parse(tmp)
   var latestEventSet: Boolean = false
   var prioEventBuffer = new ListBuffer[Map[String, String]]()
   events.foreach{ e =>
     if (isValidTriageColor(e("Title"))) {
       val startOfEvent = formatter.parse(e("Start").replaceAll("Z$", "+0000"))
       if (latestEventSet) {
         if (startOfEvent.after(startOfLatestEvent)) {
           startOfLatestEvent = startOfEvent
           latestEventString = e("Start")
           prio = e("Title")
         }
       } else {
         startOfLatestEvent = startOfEvent
         latestEventString = e("Start")
         prio = e("Title")
         latestEventSet = true
       }
     }
   }
   if (prio != "NA" && latestEventString != "NA") {
     return updatePriority(careContactId, latestEventString, prio)
   }
   return apiPatient.Undefined()
 }*/

 /**
 Discerns team and clinic, returns a apiPatient.Team-type.
 *//**
 def updateTeam(careContactId: String, timestamp: String, team: String, reasonForVisit: String, location: String, oldTeam: String, oldReasonForVisit: String): apiPatient.Team = {
   if (oldTeam == "given" && oldReasonForVisit == "given") {
     return apiPatient.Team(decodeTeamWithOldGiven(location, team, reasonForVisit), decodeClinic(team), reasonForVisit, timestamp)
   } else if (oldTeam == "given" && oldReasonForVisit == "new" && location == "NA") {
     return apiPatient.Team(decodeTeamWithReasonForVisit(team, reasonForVisit), decodeClinic(team), reasonForVisit, timestamp)
   } else if (oldTeam == "given" && oldReasonForVisit == "new") {
     return apiPatient.Team(decodeTeamWithReasonForVisitAndLocation(team, location, reasonForVisit), decodeClinic(team), reasonForVisit, timestamp)
   } else if (oldTeam == "new" && oldReasonForVisit == "new") {
     return apiPatient.Team(decodeTeamWithReasonForVisitAndLocationAndTeam(team, location, reasonForVisit), decodeClinic(team), reasonForVisit, timestamp)
   } else {
     if (reasonForVisit != "NA" && location != "NA") {
       return apiPatient.Team(decodeTeamWithReasonForVisitAndLocationAndClinic(reasonForVisit, location, team), decodeClinic(team), reasonForVisit, timestamp)
     } else if (location != "NA") {
       return apiPatient.Team(decodeTeamWithLocation(location, team, oldTeam, oldReasonForVisit), decodeClinic(team), oldReasonForVisit, timestamp)
     } else {
       return apiPatient.Team(decodeTeam(team, oldTeam, oldReasonForVisit), decodeClinic(team), oldReasonForVisit, timestamp)
     }
   }
 }*/

 /**
 Calculates the time diff. in milliseconds between arrival time and now, returns an apiPatient.ArrivalTime-type.
 *//**
 def updateArrivalTime(careContactId: String, timestamp: String): apiPatient.ArrivalTime = {
   return apiPatient.ArrivalTime(getArrivalFormat(timestamp), timestamp)
 }*/

 /**
 Cleans up apiPatient.Location-value and returns a Location-type.
 *//**
 def updateLocation(careContactId: String, timestamp: String, location: String): apiPatient.Location = {
   return apiPatient.Location(decodeLocation(location), timestamp)
 }*/

 /**
 Checks if patient has been attended by doctor, returns an Attended-type.
 *//**
 def updateAttended(careContactId: String, events: List[Map[String, String]]): apiPatient.Attended = {
   events.foreach{ e =>
     if (e("Title") == "Läkare" || e("Title") == "läkare") {
       return apiPatient.Attended(true, e("Value"), e("Start"))
     }
   }
   return apiPatient.Attended(false, "NA", "")
 }*/

 /**
 Discerns priority and returns corresponding PriorityEvent-type.
 *//**
 def updatePriority(careContactId: String, timestamp: String, priority: String): apiPatient.Priority = {
   priority match {
     case "Blå" => apiPatient.Priority("Blue", timestamp)
     case "Grön" => apiPatient.Priority("Green", timestamp)
     case "Gul" =>  apiPatient.Priority("Yellow", timestamp)
     case "Orange" => apiPatient.Priority("Orange", timestamp)
     case "Röd" => apiPatient.Priority("Red", timestamp)
     case _ => apiPatient.Priority("NotTriaged", timestamp)
   }
 }*/

 /**
 * Checks if the patient has a "plan", returns apiPatient.Plan-type.
 *//**
 def updatePlan(careContactId: String, events: List[Map[String, String]]): apiPatient.Plan = {
   events.foreach{ e =>
     if (e("Title") == "Plan") {
       return apiPatient.Plan(true, e("Start"))
     }
   }
   return apiPatient.Plan(false, "")
 }*/

 /**
 * Determines if the patient is currently on examination, returns Examination-type.
 *//**
 def updateExamination(careContactId: String, events: List[Map[String, String]]): apiPatient.Examination = {
   events.foreach{ e =>
     if (e("Title") == "Rö/klin" && e("End") == "0001-01-02T23:00:00Z") {
       return apiPatient.Examination(true, e("Start"))
     }
   }
   return apiPatient.Examination(false, "")
 }*/

 // /**
 // * Updates patient with latest clinic and reasonForVisit
 // */
 // def updateDebugging(careContactId: String, events: List[Map[String, String]]): apiPatient.Debugging = {
 //   events.foreach{ e =>
 //     if (e("Title") == "Rö/klin" && e("End") == "0001-01-02T23:00:00Z") {
 //       return apiPatient.Debugging(true, e("Start"))
 //     }
 //   }
 //   return apiPatient.Examination(false, "")
 // }

 /**
 Filters out a room nr to present from an apiPatient.Location.
 *//**
 def decodeLocation(location: String): String = {
   if (location matches "[GgBbPp]([Tt]|[Ii])[1,4]") {
     location.replaceAll("[^0-9]","")
   } else if (location matches "[BbGgPp].{2}") {
     location.replaceAll("[^0-9]","")
   } else location
 }*/

 /**
 * Discerns clinic from Team-field.
 * Used by updateTeam().
 *//**
 def decodeClinic(team: String): String = {
   team match {
     case "NAKKI" => "kirurgi"
     case "NAKME" | "NAKM" => "medicin"
     case "NAKOR" => "ortopedi"
     case "NAKBA" | "NAKGY" | "NAKÖN" => "bgö"
     case _ => "bgö"
   }
 }*/
/**
 def decodeTeamWithReasonForVisitAndLocationAndTeam(clinic: String, location: String, reasonForVisit: String): String = {
   reasonForVisit match {
     case "AKP" => "stream"
     case "ALL" | "TRAU" => "process"
     case "B" | "MEP" => {
       clinic match {
         case "NAKME" => {
           if (location != "") {
             location.charAt(0) match {
               case 'B' => "medicin blå"
               case 'G' => "medicin gul"
               case 'P' => "process"
               case _ => "medicin"
             }
           } else {
             "medicin"
           }
         }
         case "NAKKI" => "kirurgi"
         case "NAKOR" => "ortopedi"
         case "NAKBA" | "NAKGY" | "NAKÖN" => "jour"
         case "NAKM" => {
           if (location != "") {
             location.charAt(0) match {
               case 'B' => "medicin blå"
               case 'G' => "medicin gul"
               case 'P' => "process"
               case _ => "NAKM"
             }
           } else {
             "NAKM"
           }
         }
         case _ => "no-match"
       }
     }
     case _ => "no-match"
   }
 }*/

 /**
 Checks if given field is empty or not.
 *//**
 def fieldEmpty(field: String): Boolean = {
   if (field == "") {
     return true
   }
   return false
 }*/



  // A "ticker" that sends a "tick" string to self every 1 minute
  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(0 seconds, 1 minutes, self, "tick")

  /** Casts a jValue to a List[A] without crashing on empty lists */
  def castJValueToList[A](list:JValue): List[A] = {
    list match {
      case JNothing => List[A]()
      case _ => list.asInstanceOf[JArray].values.asInstanceOf[List[A]]
    }
  }

}

object ElvisDataHandlerDevice {
  def props = Props(classOf[ElvisDataHandlerDevice])
}
