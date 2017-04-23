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

class WaitingRoomDevice extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("patient-event-topic", self)

  /**
  Receives incoming messages on the AKKA-bus
  */
  def receive = {
    case mess @ _ if {log.debug(s"WaitingRoomService MESSAGE: $mess from $sender"); false} => Unit
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
    WaitingRoomComm.extractPatientEvent(mess) map { case (h, b) =>
      b match {
        case api.NewPatient(careContactId, patientData, events) => {
          val patientProperties = extractNewPatientProperties(api.NewPatient(careContactId, patientData, events))
          if (!patientProperties.isEmpty) {
            printProperties("NEW PATIENT: PatientProps to send: ", patientProperties)
            for (patientProperty <- patientProperties) {
              publishOnAkka(header, patientProperty)
            }
          }
        }
        case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
          val patientProperties = extractDiffPatientProperties(api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents))
          if (!patientProperties.isEmpty) {
            printProperties("DIFF PATIENT: PatientProps to send: ", patientProperties)
            for (patientProperty <- patientProperties) {
              publishOnAkka(header, patientProperty)
            }
          }
        }
        case api.RemovedPatient(careContactId, timestamp) => {
          val toSend = api.Finished(careContactId, timestamp)
          printProperties("REMOVED PATIENT: PatientProps to send: ", toSend)
          publishOnAkka(header, toSend)
        }
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
  Takes a NewPatient and returns PatientProperties based on patient data and events.
  */
  def extractNewPatientProperties(patient: api.NewPatient): List[api.PatientProperty] = {
    return filterNewPatientProperties(patient, getNewPatientProperties(patient))
  }

  /**
  Takes a DiffPatient and returns PatientProperties based on updates and new events.
  */
  def extractDiffPatientProperties(patient: api.DiffPatient): List[api.PatientProperty] = {
    return filterDiffPatientProperties(patient, getDiffPatientProperties(patient))
  }

  /**
  Takes a NewPatient and extracts PatientProperties based on patient data and events.
  */
  def getNewPatientProperties(patient: api.NewPatient): List[api.PatientProperty] = {
    var patientPropertyBuffer = new ListBuffer[api.PatientProperty]()
    patient.patientData.foreach{ p =>
      p._1 match {
        case "Location" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updateLocation(patient.careContactId, patient.patientData("timestamp"), p._2)
        case "Team" => if (!fieldEmpty(p._2) && !fieldEmpty(patient.patientData("Location"))) patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), p._2, patient.patientData("ReasonForVisit"), patient.patientData("Location"))
        case _ => patientPropertyBuffer += api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")
      }
    }
    patientPropertyBuffer += updateAttended(patient.careContactId, patient.events)
    return patientPropertyBuffer.toList
  }

  /**
  Takes a DiffPatient and extracts PatientProperties based on updates and events.
  */
  def getDiffPatientProperties(patient: api.DiffPatient): List[api.PatientProperty] = {
    var patientPropertyBuffer = new ListBuffer[api.PatientProperty]()
    patient.patientData.foreach{ p =>
      p._1 match {
        case "Location" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updateLocation(patient.careContactId, patient.patientData("timestamp"), p._2)
        case "Team" => if (!fieldEmpty(p._2) && !fieldEmpty(patient.patientData("Location"))) patientPropertyBuffer += updateTeam(patient.careContactId, patient.patientData("timestamp"), p._2, patient.patientData("ReasonForVisit"), patient.patientData("Location"))
        case _ => patientPropertyBuffer += api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")
      }
    }
    patientPropertyBuffer += updateAttended(patient.careContactId, patient.newEvents)
    return patientPropertyBuffer.toList
  }

  /**
  Filters out unwanted patient properties.
  */
  def filterNewPatientProperties(patient: api.NewPatient, patientProperties: List[api.PatientProperty]): List[api.PatientProperty] = {
    patientProperties
      .filter(_ != (api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")))
      .filter(_ != api.Attended(patient.careContactId, "-1", false, "NA"))
  }

  /**
  Filters out unwanted patient properties.
  */
  def filterDiffPatientProperties(patient: api.DiffPatient, patientProperties: List[api.PatientProperty]): List[api.PatientProperty] = {
    patientProperties
      .filter(_ != (api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")))
      .filter(_ != api.Attended(patient.careContactId, "-1", false, "NA"))
  }

  /**
  Discerns team and klinik, returns a Team-type.
  */
  def updateTeam(careContactId: String, timestamp: String, team: String, reasonForVisit: String, location: String): api.Team = {
    return api.Team(careContactId, timestamp, decodeTeam(reasonForVisit, location, team), decodeClinic(team))
  }

  /**
  Cleans up Location-value and returns a RoomNr-type.
  */
  def updateLocation(careContactId: String, timestamp: String, location: String): api.RoomNr = {
    return api.RoomNr(careContactId, timestamp, decodeLocation(location))
  }

  /**
  Checks if patient has been attended by doctor, returns an Attended-type.
  */
  def updateAttended(careContactId: String, events: List[Map[String, String]]): api.PatientProperty = {
    events.foreach{ e =>
      if (e("Title") == "Läkare" || e("Title") == "läkare") {
        return api.Attended(careContactId, e("Start"), true, e("Value"))
      }
      if (e("Title") == "Klar") {
        return api.FinishedStillPresent(careContactId, e("Start"))
      }
    }
    return api.Attended(careContactId, "-1", false, "NA")
  }

  /**
  Filters out a room nr or "ivr" from a location
  */
  def decodeLocation(location: String): String = {
    if (location == "ivr" || location == "yvr" || location == "vr" || location == "inv") {
      return "ivr"
    }
    return location.replaceAll("[^0-9]","")
  }

  /**
  Discerns clinic from Team-field.
  Used by updateTeam().
  */
  def decodeClinic(clinic: String): String = {
    clinic match {
      case "NAKKI" => "kirurgi"
      case "NAKME" => "medicin"
      case "NAKOR" => "ortopedi"
      case "NAKBA" | "NAKGY" | "NAKÖN" => "bgö"
      case _ => "bgö" // dont know what this means, ex NAK23T
    }
  }

  /**
  Discerns team from ReasonForVisit and Location, and Team fields.
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
  Checks if given field is empty or not.
  */
  def fieldEmpty(field: String): Boolean = {
    if (field == "") {
      return true
    }
    return false
  }

  /**
  Publishes a SPMessage on bus with header and body
  */
  def publishOnAkka(header: SPHeader, body: api.PatientProperty) {
    val toSend = WaitingRoomComm.makeMess(header, body)
    toSend match {
      case Success(v) =>
        mediator ! Publish("waiting-room-widget-topic", v) // Publishes on bus for widget to receive
      case Failure(e) =>
        println("Failed")
    }
  }

  }

  object WaitingRoomDevice {
    def props = Props(classOf[WaitingRoomDevice])
  }
