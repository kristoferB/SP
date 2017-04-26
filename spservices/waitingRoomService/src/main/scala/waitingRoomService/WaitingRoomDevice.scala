package sp.waitingroomservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success, Failure }

import java.util.Date
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.chrono.ChronoLocalDate

import scala.collection.mutable.ListBuffer

import sp.waitingroomservice.{API_PatientEvent => api}
import sp.waitingroomservice.{API_Patient => apiPatient}

class WaitingRoomDevice extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  //mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("patient-event-topic", self)
  mediator ! Subscribe("waiting-room-service-topic", self)

  var state: Map[String, apiPatient.Patient] = Map()
  var widgetStarted: Boolean = false

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

  /**
  Identifies the body of the SP-message and acts correspondingly
  */
  def matchRequests(mess: Try[SPMessage]) = {
    val header = SPHeader(from = "waitingRoomService")
    WaitingRoomComm.extractEvent(mess) map { case (h, b) =>
      b match {
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
        case api.GetState() => {
          println("Got state request from " + h.from)
          widgetStarted = true
          publishOnAkka(header, api.State(state))
        }
        case _ => println("Not expected")
      }
    }
  }

  /**
  * Updates all time differences + 1 min.
  */
  def updateAllTimeDiffs() {
    state.foreach{ p =>
      if (p._2.latestEvent.timeDiff != -1) {
        updateState(p._1, apiPatient.LatestEvent(p._2.latestEvent.latestEvent, p._2.latestEvent.timeDiff + 60000, p._2.latestEvent.timestamp)) // 60 000 ms is one minute
      }
    }
  }

  /**
  * Prints what is about to be sent on bus.
  */
  def printProperties(firstRow: String, secondRow: Any) {
    println(firstRow)
    println(secondRow)
    println()
    println()
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
    if (widgetStarted) {
      publishOnAkka(SPHeader(from = "waitingRoomService"), api.State(state))
    }
  }

  /**
  * Constructs a new patient object.
  */
  def updateNewPatient(ccid: String, prop: apiPatient.PatientProperty): apiPatient.Patient = {
    prop match {
      case apiPatient.Priority(color, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority(color, timestamp), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
      case apiPatient.Attended(attended, doctorId, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("", ""), apiPatient.Attended(attended, doctorId, timestamp), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
      case apiPatient.Location(roomNr, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("", ""), apiPatient.Attended(false, "", ""), apiPatient.Location(roomNr, timestamp), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
      case apiPatient.Team(team, clinic, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team(team, clinic, timestamp), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
      case apiPatient.Examination(isOnExam, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(isOnExam, timestamp), apiPatient.LatestEvent("", -1, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
      case apiPatient.LatestEvent(latestEvent, timeDiff, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent(latestEvent, -1, timestamp), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
      case apiPatient.ArrivalTime(timeDiff, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, ""), apiPatient.ArrivalTime(timeDiff, timestamp), apiPatient.FinishedStillPresent(false, ""))
      case apiPatient.FinishedStillPresent(finishedStillPresent, timestamp) => apiPatient.Patient(ccid, apiPatient.Priority("", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(finishedStillPresent, timestamp))
      case _ => apiPatient.Patient(ccid, apiPatient.Priority("", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
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
      case apiPatient.LatestEvent(latestEvent, timeDiff, timestamp) => apiPatient.Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, apiPatient.LatestEvent(latestEvent, timeDiff, timestamp), s(ccid).arrivalTime, s(ccid).finishedStillPresent)
      case apiPatient.ArrivalTime(timeDiff, timestamp) => apiPatient.Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, apiPatient.ArrivalTime(timeDiff, timestamp), s(ccid).finishedStillPresent)
      case apiPatient.FinishedStillPresent(finishedStillPresent, timestamp) => apiPatient.Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).examination, s(ccid).latestEvent, s(ccid).arrivalTime, apiPatient.FinishedStillPresent(finishedStillPresent, timestamp))
      case _ => apiPatient.Patient(ccid, apiPatient.Priority("", ""), apiPatient.Attended(false, "", ""), apiPatient.Location("", ""), apiPatient.Team("", "", ""), apiPatient.Examination(false, ""), apiPatient.LatestEvent("", -1, ""), apiPatient.ArrivalTime("", ""), apiPatient.FinishedStillPresent(false, ""))
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
    patientPropertyBuffer += updateLatestEvent(patient.careContactId, patient.events)
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
    patientPropertyBuffer += updateLatestEvent(patient.careContactId, patient.newEvents)
    return patientPropertyBuffer.toList
  }

  /**
  Filters out unwanted patient properties.
  */
  def filterNewPatientProperties(patient: api.NewPatient, patientProperties: List[apiPatient.PatientProperty]): List[apiPatient.PatientProperty] = {
    patientProperties
      .filter(_ != apiPatient.Undefined())
      .filter(_ != apiPatient.LatestEvent("", -1, ""))
      .filter(_ != apiPatient.Attended(false, "NA", ""))
  }

  /**
  Filters out unwanted patient properties.
  */
  def filterDiffPatientProperties(patient: api.DiffPatient, patientProperties: List[apiPatient.PatientProperty]): List[apiPatient.PatientProperty] = {
    patientProperties
      .filter(_ != apiPatient.Undefined())
      .filter(_ != apiPatient.LatestEvent("", -1, ""))
      .filter(_ != apiPatient.Attended(false, "NA", ""))
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
  Checks if patient has been apiPatient.Attended by doctor, returns an apiPatient.Attended-type.
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
  Discerns apiPatient.Priority and returns corresponding apiPatient.PriorityEvent-type.
  */
  def updatePriority(careContactId: String, timestamp: String, priority: String): apiPatient.Priority = {
    priority match {
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
  def updateLatestEvent(careContactId: String, events: List[Map[String, String]]): apiPatient.LatestEvent = {
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
      return apiPatient.LatestEvent(title, timeDiff, timestampString)
    }
    return apiPatient.LatestEvent("", -1, "")
  }

  /**
  Filters out a room nr or "ivr" from a apiPatient.Location
  */
  def decodeLocation(location: String): String = {
    if (location contains "ivr") {
      return "ivr"
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
      case "B" => {
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
    if (string == "Grön" || string == "Gul" || string == "Orange" || string == "Röd") {
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

  /**
  Publishes a SPMessage on bus with header and body.
  */
  def publishOnAkka(header: SPHeader, body: api.StateEvent) {
    val toSend = WaitingRoomComm.makeMess(header, body)
    toSend match {
      case Success(v) =>
        println("Published current state to waiting-room-widget-topic")
        mediator ! Publish("waiting-room-widget-topic", v) // Publishes on bus for widget to receive
      case Failure(e) =>
        println("Failed")
    }
  }

  // A "ticker" that sends a "tick" string to self every 1 minute
  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(0 seconds, 1 minutes, self, "tick")

  }

  object WaitingRoomDevice {
    def props = Props(classOf[WaitingRoomDevice])
  }
