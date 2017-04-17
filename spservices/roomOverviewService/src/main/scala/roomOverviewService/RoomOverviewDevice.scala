package sp.roomoverviewservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success, Failure }

import scala.collection.mutable.ListBuffer

import java.util.Date
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.chrono.ChronoLocalDate

import sp.roomoverviewservice.{API_PatientEvent => api}

class RoomOverviewDevice extends Actor with ActorLogging {
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
    case mess @ _ if {log.debug(s"RoomOverviewService MESSAGE: $mess from $sender"); false} => Unit
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
    val header = SPHeader(from = "roomOverviewService")
    RoomOverviewComm.extractPatientEvent(mess) map { case (h, b) =>
      b match {
        case api.NewPatient(careContactId, patientData, events) => {
          val patientProperties = extractNewPatientProperties(api.NewPatient(careContactId, patientData, events))
          if (!patientProperties.isEmpty) {
            for (patientProperty <- patientProperties) {
              publishOnAkka(header, patientProperty)
            }
          }
        }
        case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
          val patientProperties = extractDiffPatientProperties(api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents))
          if (!patientProperties.isEmpty) {
            for (patientProperty <- patientProperties) {
              publishOnAkka(header, patientProperty)
            }
          }
        }
        case api.RemovedPatient(careContactId, timestamp) => {
          println("REMOVED PATIENT: PatientProps to send: ")
          val toSend = api.Finished(careContactId, timestamp)
          println(toSend)
          println()
          println()
          publishOnAkka(header, toSend)
        }
      }
    }
  }

  /**
  Takes a NewPatient and returns PatientProperties based on patient data and events.
  */
  def extractNewPatientProperties(patient: api.NewPatient): List[api.PatientProperty] = {
    val filteredPatientProps = filterNewPatientProperties(patient, getNewPatientProperties(patient))
    println("NEW PATIENT: PatientProps to send: ")
    println(filteredPatientProps)
    println()
    println()
    return filteredPatientProps
  }

  /**
  Takes a DiffPatient and returns PatientProperties based on updates and new events.
  */
  def extractDiffPatientProperties(patient: api.DiffPatient): List[api.PatientProperty] = {
    val filteredPatientProps = filterDiffPatientProperties(patient, getDiffPatientProperties(patient))
    println("DIFF PATIENT: PatientProps to send: ")
    println(filteredPatientProps)
    println()
    println()
    return filteredPatientProps
  }

  /**
  Takes a NewPatient and extracts PatientProperties based on patient data and events.
  */
  def getNewPatientProperties(patient: api.NewPatient): List[api.PatientProperty] = {
    var patientPropertyBuffer = new ListBuffer[api.PatientProperty]()
    patient.patientData.foreach{ p =>
      p._1 match {
        case "Location" => if (!fieldEmpty(p._2)) patientPropertyBuffer += updateLocation(patient.careContactId, patient.patientData("timestamp"), p._2)
        case _ => patientPropertyBuffer += api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")
      }
    }
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
        case _ => patientPropertyBuffer += api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")
      }
    }
    return patientPropertyBuffer.toList
  }

  /**
  Filters out unwanted patient properties.
  */
  def filterNewPatientProperties(patient: api.NewPatient, patientProperties: List[api.PatientProperty]): List[api.PatientProperty] = {
    patientProperties
      .filter(_ != (api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")))
      .filter(_ != api.LatestEvent(patient.careContactId, "-1", "NA", -1))
      .filter(_ != api.Attended(patient.careContactId, "-1", false, "NA"))
  }

  /**
  Filters out unwanted patient properties.
  */
  def filterDiffPatientProperties(patient: api.DiffPatient, patientProperties: List[api.PatientProperty]): List[api.PatientProperty] = {
    patientProperties
      .filter(_ != (api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")))
      .filter(_ != api.LatestEvent(patient.careContactId, "-1", "NA", -1))
      .filter(_ != api.Attended(patient.careContactId, "-1", false, "NA"))
  }

  /**
  Cleans up Location-value and returns a RoomNr-type.
  */
  def updateLocation(careContactId: String, timestamp: String, location: String): api.RoomNr = {
    api.RoomNr(careContactId, timestamp, decodeLocation(location))
  }

  /**
  Filters out a room nr or "ivr" from a location
  */
  def decodeLocation(location: String): String = {
    if (location contains "ivr") {
      return "ivr"
    }
    return location.replaceAll("[^0-9]","")
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
    val toSend = RoomOverviewComm.makeMess(header, body)
    toSend match {
      case Success(v) =>
        mediator ! Publish("room-overview-widget-topic", v) // Publishes on bus for widget to receive
      case Failure(e) =>
        println("Failed")
    }
  }

  }

  object RoomOverviewDevice {
    def props = Props(classOf[RoomOverviewDevice])
  }
