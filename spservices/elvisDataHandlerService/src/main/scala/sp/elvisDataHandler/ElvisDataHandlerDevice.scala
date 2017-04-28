package sp.elvisdatahandler

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
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

import elastic.PatientsToElastic
import elastic.GetFromElastic

import sp.elvisdatahandler.{API_PatientEvent => api}
import sp.elvisdatahandler.{API_Patient => apiPatient}


/**
*  This is the actor (the service) that listens for messages on the bus
*/
class ElvisDataHandlerDevice extends Actor with ActorLogging {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization

  val patientsToElastic = new PatientsToElastic
  //val getFromElastic = new GetFromElastic

  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("felvis-data-topic", self)
  mediator ! Subscribe("elvis-data-topic", self)
  mediator ! Subscribe("elvis-diff", self)

  var state: Map[String, apiPatient.Patient] = Map()

  /**
  Receives incoming messages on the AKKA-bus
  */
  def receive = {
    case "tick" => updateAllTimeDiffs()
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
      case api.ElvisData(s) =>
        patientsToElastic.messageReceived(s) // Insert data to database
        handlePatient(handleMessage(s))
    }
  }
}

val info = SPAttributes(
   "service" -> api.attributes.service,
   "group" -> "runtime"
 )

 /**
 * Acts according to which kind of patient was received (new, diff or removed).
 */
 def handlePatient(patient: api.PatientEvent) {
   patient match {
     case api.NewPatient(careContactId, patientData, events) => {
       val patientProperties = extractNewPatientProperties(api.NewPatient(careContactId, patientData, events))
       if (!patientProperties.isEmpty) {
         for (patientProperty <- patientProperties) {
           updateState(careContactId, patientProperty)
         }
       }
     }
     case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
       val patientProperties = extractDiffPatientProperties(api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents))
       if (!patientProperties.isEmpty) {
         for (patientProperty <- patientProperties) {
           updateState(careContactId, patientProperty)
         }
       }
     }
     case api.RemovedPatient(careContactId, timestamp) => {
       updateState(careContactId, apiPatient.Finished())
     }
     case _ => println("Not expected")
   }
 }

 /**
 * Updates all time differences + 1 min.
 */
 def updateAllTimeDiffs() {
   state.foreach{ p =>
     if (p._2.latestEvent.timeDiff != -1) {
       updateState(p._1, apiPatient.LatestEvent(p._2.latestEvent.latestEvent, p._2.latestEvent.timeDiff + 60000, p._2.latestEvent.needsAttention, p._2.latestEvent.timestamp)) // 60 000 ms is one minute
     }
   }
 }

 /**
 * Updates the current state based on what patient property is received.
 */
 def updateState(careContactId: String, prop: apiPatient.PatientProperty) {
   if (state.keys.exists(_ == careContactId)) {
     if (prop.isInstanceOf[apiPatient.Finished]) {
       state -= careContactId
     } else {
       state += (careContactId -> updateExistingPatient(state, careContactId, prop))
     }
   } else {
     state += (careContactId -> updateNewPatient(careContactId, prop))
   }
   publishOnAkka(SPHeader(from = "elvisDataHandler"), api.State(state))
 }

 /**
 * Constructs a new patient object.
 */
 def updateNewPatient(ccid: String, prop: apiPatient.PatientProperty): apiPatient.Patient = {
   prop match {
     case apiPatient.Priority(color, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority(color, timestamp), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
     case apiPatient.Attended(attended, doctorId, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(attended, doctorId, timestamp), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
     case apiPatient.Location(roomNr, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "", ""), apiPatient.Location(roomNr, timestamp), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
     case apiPatient.Team(team, clinic, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team(team, clinic, timestamp), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
     case apiPatient.Examination(isOnExam, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(isOnExam, timestamp), apiPatient.LatestEvent("", -1, false, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
     case apiPatient.LatestEvent(latestEvent, timeDiff, needsAttention, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent(latestEvent, timeDiff, needsAttention, timestamp), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
     case apiPatient.ArrivalTime(timeDiff, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.ArrivalTime(timeDiff, timestamp), apiPatient.FinishedStillPresent(false, ""))
     case apiPatient.FinishedStillPresent(finishedStillPresent, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(finishedStillPresent, timestamp))
     case _ => apiPatient.Patient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
   }
 }

 /**
 * Constructs an updates patient object.
 */
 def updateExistingPatient(s: Map[String, apiPatient.Patient], ccid: String, prop: apiPatient.PatientProperty): apiPatient.Patient = {
   prop match {
     case apiPatient.Priority(color, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority(color, timestamp), s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).arrivalTime, s(ccid).finishedStillPresent)
     case apiPatient.Attended(attended, doctorId, timestamp) => apiPatient.Patient(ccid, s(ccid).priority, apiPatient.Attended(attended, doctorId, timestamp), s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).arrivalTime, s(ccid).finishedStillPresent)
     case apiPatient.Location(roomNr, timestamp) => apiPatient.Patient(ccid, s(ccid).priority, s(ccid).attended, apiPatient.Location(roomNr, timestamp), s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).arrivalTime, s(ccid).finishedStillPresent)
     case apiPatient.Team(team, clinic, timestamp) => apiPatient.Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, apiPatient.Team(team, clinic, timestamp), s(ccid).examination, s(ccid).latestEvent, s(ccid).arrivalTime, s(ccid).finishedStillPresent)
     case apiPatient.Examination(isOnExam, timestamp) => apiPatient.Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, apiPatient.Examination(isOnExam, timestamp), s(ccid).latestEvent, s(ccid).arrivalTime, s(ccid).finishedStillPresent)
     case apiPatient.LatestEvent(latestEvent, timeDiff, needsAttention, timestamp) => apiPatient.Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, apiPatient.LatestEvent(latestEvent, timeDiff, needsAttention, timestamp), s(ccid).arrivalTime, s(ccid).finishedStillPresent)
     case apiPatient.ArrivalTime(timeDiff, timestamp) => apiPatient.Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, apiPatient.ArrivalTime(timeDiff, timestamp), s(ccid).finishedStillPresent)
     case apiPatient.FinishedStillPresent(finishedStillPresent, timestamp) => apiPatient.Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).arrivalTime, apiPatient.FinishedStillPresent(finishedStillPresent, timestamp))
     case _ => apiPatient.Patient(ccid, apiPatient.Priority("NotTriaged", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, false, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
   }
 }

 /**
 Takes a NewPatient and returns PatientProperties based on patient data and events.
 */
 def extractNewPatientProperties(patient: api.NewPatient): List[apiPatient.PatientProperty] = {
   return filterNewPatientProperties(patient, getNewPatientProperties(patient))
 }

 /**
 Takes a DiffPatient and returns PatientProperties based on updates and new events.
 */
 def extractDiffPatientProperties(patient: api.DiffPatient): List[apiPatient.PatientProperty] = {
   return filterDiffPatientProperties(patient, getDiffPatientProperties(patient))
 }

 /**
 Takes a NewPatient and extracts PatientProperties based on patient data and events.
 */
 def getNewPatientProperties(patient: api.NewPatient): List[apiPatient.PatientProperty] = {
   var patientPropertyBuffer = new ListBuffer[apiPatient.PatientProperty]()
   patient.patientData.foreach{ p =>
     p._1 match {
       case "Team" => if (!fieldEmpty(p._2) && !fieldEmpty(patient.patientData("Location"))) patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), p._2, patient.patientData("ReasonForVisit"), patient.patientData("Location"))
       case "Location" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updateLocation(patient.careContactId, patient.patientData("timestamp"), p._2)
       case "Priority" => patientPropertyBuffer += updatePriority(patient.careContactId, patient.patientData("timestamp"), p._2)
       case "VisitRegistrationTime" => patientPropertyBuffer += updateArrivalTime(patient.careContactId, p._2)
       case _ => patientPropertyBuffer += apiPatient.Undefined()
     }
   }
   patientPropertyBuffer += updateAttended(patient.careContactId, patient.events)
   patientPropertyBuffer += updateLatestEvent(patient.careContactId, patient.events, patient.patientData("Priority"))
   patientPropertyBuffer += updateExamination(patient.careContactId, patient.events)
   patientPropertyBuffer += updateFinishedStillPresent(patient.careContactId, patient.events)
   return patientPropertyBuffer.toList
 }

 /**
 Takes a DiffPatient and extracts PatientProperties based on updates and events.
 */
 def getDiffPatientProperties(patient: api.DiffPatient): List[apiPatient.PatientProperty] = {
   var patientPropertyBuffer = new ListBuffer[apiPatient.PatientProperty]()
   patient.patientData.foreach{ p =>
     p._1 match {
       case "Team" => if (!fieldEmpty(p._2) && !fieldEmpty(patient.patientData("Location"))) patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), p._2, patient.patientData("ReasonForVisit"), patient.patientData("Location"))
       case "Location" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updateLocation(patient.careContactId, patient.patientData("timestamp"), p._2)
       case _ => patientPropertyBuffer += apiPatient.Undefined()
     }
   }
   patient.newEvents.foreach{ e =>
     if (e("Title") == "Läkare") {
       patientPropertyBuffer += apiPatient.Attended(true, e("Value"), e("Start"))
     }
   }
   patientPropertyBuffer += getLatestPrioEvent(patient.careContactId, patient.newEvents)
   patientPropertyBuffer += updateLatestEvent(patient.careContactId, patient.newEvents, patient.patientData("Priority"))
   patientPropertyBuffer += updateExamination(patient.careContactId, patient.newEvents)
   patientPropertyBuffer += updateFinishedStillPresent(patient.careContactId, patient.newEvents)
   return patientPropertyBuffer.toList
 }

 /**
 Filters out unwanted patient properties.
 */
 def filterNewPatientProperties(patient: api.NewPatient, patientProperties: List[apiPatient.PatientProperty]): List[apiPatient.PatientProperty] = {
   patientProperties
     .filter(_ != apiPatient.Undefined())
 }

 /**
 Filters out unwanted patient properties.
 */
 def filterDiffPatientProperties(patient: api.DiffPatient, patientProperties: List[apiPatient.PatientProperty]): List[apiPatient.PatientProperty] = {
   patientProperties
     .filter(_ != apiPatient.Undefined())
 }

 /**
 Identifies the latest event considering triage and returns corresponding apiPatient.PriorityEvent.
 OBS: revidera
 */
 def getLatestPrioEvent(careContactId: String, events: List[Map[String, String]]): apiPatient.PatientProperty = {
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
 }

 /**
 Discerns apiPatient.Team and klinik, returns a apiPatient.Team-type.
 */
 def updateTeam(careContactId: String, timestamp: String, team: String, reasonForVisit: String, location: String): apiPatient.Team = {
   return apiPatient.Team(decodeTeam(reasonForVisit, location, team), decodeClinic(team), timestamp)
 }

 /**
 Calculates the time diff. in milliseconds between arrival time and now, returns an apiPatient.ArrivalTime-type.
 */
 def updateArrivalTime(careContactId: String, timestamp: String): apiPatient.ArrivalTime = {
   return apiPatient.ArrivalTime(getArrivalFormat(timestamp), timestamp)
 }

 /**
 Cleans up apiPatient.Location-value and returns a Location-type.
 */
 def updateLocation(careContactId: String, timestamp: String, location: String): apiPatient.Location = {
   return apiPatient.Location(decodeLocation(location), timestamp)
 }

 /**
 Checks if patient has been attended by doctor, returns an Attended-type.
 */
 def updateAttended(careContactId: String, events: List[Map[String, String]]): apiPatient.Attended = {
   events.foreach{ e =>
     if (e("Title") == "Läkare" || e("Title") == "läkare") {
       return apiPatient.Attended(true, e("Value"), e("Start"))
     }
   }
   return apiPatient.Attended(false, "NA", "")
 }

 /**
 Discerns priority and returns corresponding PriorityEvent-type.
 */
 def updatePriority(careContactId: String, timestamp: String, priority: String): apiPatient.Priority = {
   priority match {
     case "Blå" => apiPatient.Priority("Blue", timestamp)
     case "Grön" => apiPatient.Priority("Green", timestamp)
     case "Gul" =>  apiPatient.Priority("Yellow", timestamp)
     case "Orange" => apiPatient.Priority("Orange", timestamp)
     case "Röd" => apiPatient.Priority("Red", timestamp)
     case _ => apiPatient.Priority("NotTriaged", timestamp)
   }
 }

 /**
 Returns the time difference (in milliseconds) between a given start time and now.
 Argument startTimeString must be received in date-time-format: yyyy-MM-ddTHH:mm:ssZ
 */
 def getTimeDiff(startTimeString: String): Long = {
   var formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
   val startTime = formatter.parse(startTimeString.replaceAll("Z$", "+0000"))
   val now: Long = System.currentTimeMillis
   return Math.abs(now - startTime.getTime()) // returns diff in millisec
 }

 /**
 * Returns "hh:mm (day)"-format of argument given in milliseconds.
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
     case 1 => dayString = "(igår)"
     case 2 => dayString = "(förrgår)"
     case _ => dayString = ""
   }
   return timeString + " " + dayString
 }

 /**
 Identifies the latest event if there is any in the events list, returns apiPatient.LatestEvent-type.
 */
 def updateLatestEvent(careContactId: String, events: List[Map[String, String]], priority: String): apiPatient.LatestEvent = {
   var eventFound: Boolean = false
   var timeDiff: Long = Long.MaxValue
   var title = ""
   var timestampString = ""
   // Get the latest event title and its time diff
   events.foreach{ e =>
     val startOfEventString = e("Start")
     val diff = getTimeDiff(startOfEventString)
     if (diff < timeDiff) {
       timeDiff = diff
       timestampString = startOfEventString
       eventFound = true
       if (e("Title") == "" || e("Title") == " ") {
         title = "> ej angett <"
       } else {
         if (e("Title") == "Grön" || e("Title") == "Gul" || e("Title") == "Orange" || e("Title") == "Röd") {
           title = "Triage"
         } else {
           title = e("Title")
         }
       }
     }
   }
   if (eventFound) {
     if (needAttention(timeDiff, priority)) {
       return apiPatient.LatestEvent(title, timeDiff, true, timestampString)
     } else {
       return apiPatient.LatestEvent(title, timeDiff, false, timestampString)
     }
   }
   return apiPatient.LatestEvent("", -1, false, "")
 }

 /**
 * Determines if the patient is currently on examination, returns Examination-type.
 */
 def updateExamination(careContactId: String, events: List[Map[String, String]]): apiPatient.Examination = {
   events.foreach{ e =>
     if (e("Title") == "Rö/klin" && e("End") != "0001-01-02T23:00:00Z") {
       return apiPatient.Examination(true, e("Start"))
     }
   }
   return apiPatient.Examination(false, "")
 }

 /**
 * Determines if the patient is finished but still present, returns FinishedStillPresent-type.
 */
 def updateFinishedStillPresent(careContactId: String, events: List[Map[String, String]]): apiPatient.FinishedStillPresent = {
   events.foreach{ e =>
     if (e("Title") == "Klar") {
       return apiPatient.FinishedStillPresent(true, e("Start"))
     }
   }
   return apiPatient.FinishedStillPresent(false, "")
 }

 /**
 * Checks if patient needs attention, according to some specific guidelines.
 */
 def needAttention(timeDiff: Long, priority: String): Boolean = {
   val minutes = ((timeDiff / (1000*60)) % 60)
   val hours   = ((timeDiff / (1000*60*60)) % 24)
   priority match {
     case "Green" => {
       if (hours > 0 || minutes == 60) return true
     }
     case "Yellow" => {
       if (hours > 0 || minutes == 60) return true
     }
     case "Orange" => {
       if (hours > 0 || minutes >= 20) return true
     }
     case "Red" => return true
     case _ => return false
   }
   return false
 }

 /**
 Filters out a room nr or "ivr" from a apiPatient.Location
 */
 def decodeLocation(location: String): String = {
   if (location == "ivr" || location == "yvr" || location == "gvr" || location == "bvr" || location == "iv" || location == "vr") {
     return ""
   }
   return location.replaceAll("[^0-9]","")
 }

 /**
 Discerns clinic from apiPatient.Team-field.
 Used by updateapiPatient.Team().
 */
 def decodeClinic(team: String): String = {
   team match {
     case "NAKKI" => "kirurgi"
     case "NAKME" => "medicin"
     case "NAKOR" => "ortopedi"
     case "NAKBA" | "NAKGY" | "NAKÖN" => "bgö"
     case _ => "bgö"
   }
 }

 /**
 Discerns apiPatient.Team from ReasonForVisit and apiPatient.Location, and apiPatient.Team fields.
 */
 def decodeTeam(reasonForVisit: String, location: String, team: String): String = {
   reasonForVisit match {
     case "AKP" => "stream"
     case "ALL" | "TRAU" => "process"
     case "B" | "MEP" => {
       team match {
         case "NAKME" => {
           location.charAt(0) match {
             case 'B' => "medicin blå"
             case 'G' => "medicin gul"
             case 'P' => "process"
             case _ => "medicin"
           }
         }
         case "NAKKI" => "kirurgi"
         case "NAKOR" => "ortopedi"
         case "NAKBA" | "NAKGY" | "NAKÖN" => "jour"
         case _ => "no-match"
       }
     }
     case _ => "no-match"
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

 /**
 Checks if given field is empty or not.
 */
 def fieldEmpty(field: String): Boolean = {
   if (field == "") {
     return true
   }
   return false
 }


  def handleMessage(message: String): api.PatientEvent = {
    // figure out what sort of message we just received
    val json: JValue = parse(message)
    (json \ "isa").values.toString match {
      case "newLoad" | "new" => newPatient(json)
      case "diff" => diffPatient(json)
      case "removed" => removedPatient(json)
      case _ => api.Undefined()
    }
  }

  def newPatient(json: JValue): api.NewPatient = {
    val header = SPHeader(from = "elvisDataHandlerService")
    val patientJson = patientsToElastic.initiatePatient(json \ "data" \ "patient")
    val careContactId = (patientJson \ "CareContactId").values.toString
    var patientData = extractNewPatientData(patientJson)
    val timestamp = (json \ "data" \ "timestamp").values.toString
    patientData += ("timestamp" -> timestamp)
    val events = extractNewPatientEvents(patientJson)
    return api.NewPatient(careContactId, patientData, events)
  }

  def diffPatient(json: JValue): api.DiffPatient = {
    val header = SPHeader(from = "elvisDataHandlerService")
    val careContactId = (json \ "data" \ "updates" \ "CareContactId").values.toString
    val patientData = extractDiffPatientData(json \ "data" \ "updates")
    val newEvents = extractNewEvents(json \ "data")
    val removedEvents = extractRemovedEvents(json \ "data")
    return api.DiffPatient(careContactId, patientData, newEvents, removedEvents)
  }

  def removedPatient(json: JValue): api.RemovedPatient = {
    val header = SPHeader(from = "elvisDataHandlerService")
    val careContactId = (json \ "data" \ "patient" \ "CareContactId").values.toString
    val timestamp = (json \ "data" \ "timestamp").values.toString
    return api.RemovedPatient(careContactId, timestamp)
  }

  def extractNewEvents(patient: JValue): List[Map[String, String]] = {
    var tmpList = new ListBuffer[Map[String,String]]()
    val events = castJValueToList[Map[String, JValue]](patient \ "newEvents")
    events.foreach{ m =>
      var tmpMap = Map[String, String]()
      m.foreach{ kv => tmpMap += kv._1 -> kv._2.toString
      }
      tmpList += tmpMap
    }
    return tmpList.toList
  }

  def extractRemovedEvents(patient: JValue): List[Map[String, String]] = {
    var tmpList = new ListBuffer[Map[String,String]]()
    val events = castJValueToList[Map[String, JValue]](patient \ "removedEvents")
    events.foreach{ m =>
      var tmpMap = Map[String, String]()
      m.foreach{ kv => tmpMap += kv._1 -> kv._2.toString }
      tmpList += tmpMap
    }
    return tmpList.toList
  }

  def extractNewPatientEvents(patient: JValue): List[Map[String, String]] = {
    var tmpList = new ListBuffer[Map[String,String]]()
    val events = castJValueToList[Map[String, JValue]](patient \ "Events")
    events.foreach{ m =>
      var tmpMap = Map[String,String]()
      m.foreach{ kv => tmpMap += kv._1 -> kv._2.toString }
      tmpList += tmpMap
    }
    return tmpList.toList
  }

  def extractDiffPatientData(patient: JValue): Map[String, String] = {
    var departmentComment = ""
    var location = ""
    var reasonForVisit = ""
    var team = ""

    val departmentCommentJson = (patient \ "DepartmentComment")
    val locationJson = (patient \ "Location")
    val reasonForVisitJson = (patient \ "ReasonForVisit")
    val teamJson = (patient \ "Team")

    if (departmentCommentJson != JNothing) {
      departmentComment = departmentCommentJson.values.toString
    }
    if (locationJson != JNothing) {
      location = locationJson.values.toString
    }
    if (reasonForVisitJson != JNothing) {
      reasonForVisit = reasonForVisitJson.values.toString
    }
    if (teamJson != JNothing) {
      team = teamJson.values.toString
    }

    val timestamp = (patient \ "timestamp").values.toString
    val careContactId = (patient \ "CareContactId").values.toString
    val patientId = (patient \ "PatientId").values.toString

    return Map("DepartmentComment" -> departmentComment, "Location" -> location, "ReasonForVisit" -> reasonForVisit,
    "Team" -> team, "timestamp" -> timestamp, "CareContactId" -> careContactId, "PatientId" -> patientId)
  }

  def extractNewPatientData(patient: JValue): Map[String, String] = {
    val careContactId = (patient \ "CareContactId").values.toString
    val careContactRegistrationTime = (patient \ "CareContactRegistrationTime").values.toString
    val departmentComment = (patient \ "DepartmentComment").values.toString
    val location = (patient \ "Location").values.toString
    val patientId = (patient \ "PatientId").values.toString
    val reasonForVisit = (patient \ "ReasonForVisit").values.toString
    val team = (patient \ "Team").values.toString
    val visitId = (patient \ "VisitId").values.toString
    val visitRegistrationTime = (patient \ "VisitRegistrationTime").values.toString
    val priority = (patient \ "Priority").values.toString
    val timeToDoctor = (patient \ "TimeToDoctor").values.toString
    val timeToTriage = (patient \ "TimeToTriage").values.toString
    val timeToFinished = (patient \ "TimeToFinished").values.toString
    val timeOfDoctor = (patient \ "TimeOfDoctor").values.toString
    val timeOfTriage = (patient \ "TimeOfTriage").values.toString
    val timeOfFinished = (patient \ "TimeOfFinished").values.toString

    return Map("CareContactId" -> careContactId, "CareContactRegistrationTime" -> careContactRegistrationTime,
      "DepartmentComment" -> departmentComment, "Location" -> location, "PatientId" -> patientId,
      "ReasonForVisit" -> reasonForVisit, "Team" -> team, "VisitId" -> visitId,
      "VisitRegistrationTime" -> visitRegistrationTime, "Priority" -> priority, "TimeToDoctor" -> timeToDoctor,
      "TimeToTriage" -> timeToTriage, "TimeToFinished" -> timeToFinished, "TimeOfDoctor" -> timeOfDoctor,
      "TimeOfTriage" -> timeOfTriage, "TimeOfFinished" -> timeOfFinished
    )
  }

  def publishOnAkka(header: SPHeader, body: api.Event) {
    val toSend = ElvisDataHandlerComm.makeMess(header, body)
    toSend match {
      case Success(v) =>
        mediator ! Publish("patient-event-topic", v)
      case Failure(e) =>
        println("Failed")
    }
  }

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
