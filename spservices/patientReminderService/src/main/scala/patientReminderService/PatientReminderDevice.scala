package sp.patientreminderservice

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

import sp.patientreminderservice.{API_PatientEvent => api}

class PatientReminderDevice extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("patient-event-topic", self)

  case class Patient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]])

  var activePatients: Map[String, Patient] = Map()

  def receive = {
    case mess @ _ if {log.debug(s"PatientReminderService MESSAGE: $mess from $sender"); false} => Unit

    case x: String => handleRequests(x)

  }

  def handleRequests(x: String): Unit = {
    val mess = SPMessage.fromJson(x)
    matchRequests(mess)
  }

  def matchRequests(mess: Try[SPMessage]) = {
    val header = SPHeader(from = "patientReminderService")
    PatientReminderComm.extractPatientEvent(mess) map { case (h, b) =>
      b match {
        case api.NewPatient(careContactId, patientData, events) => {
          activePatients += careContactId -> Patient(careContactId, patientData, events)
          if (!events.isEmpty && !isFinished(patientData("TimeToFinished"))) { // only send reminder if events list is not empty and the patient isnt declared finished
            println("+ New patient: " + careContactId + ", Latest event: " + getLatestEvent(events)("Title") + ", with time diff: " + getTimePrintString(getLatestEvent(events)("TimeDiff")))
            publishOnAkka(header, api.NewPatientReminder(careContactId, appendFieldsToPatientData(patientData, events)))
          }
        }
        case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
          val updatedPatientData = appendNewPatientData(careContactId, patientDataDiff)
          val updatedEvents = appendNewEvents(careContactId, newEvents)
          activePatients += careContactId -> Patient(careContactId, updatedPatientData, updatedEvents)
          if (!updatedEvents.isEmpty && !isFinished(updatedPatientData("TimeToFinished"))) { // only send reminder if events list is not empty and the patient isnt declared finished
            println("Diff patient: " + careContactId + ", Latest event: " + getLatestEvent(updatedEvents)("Title") + ", with time diff: " + getTimePrintString(getLatestEvent(updatedEvents)("TimeDiff")))
            publishOnAkka(header, api.NewPatientReminder(careContactId, appendFieldsToPatientData(updatedPatientData, updatedEvents)))
          }
        }
        case api.RemovedPatient(careContactId) => {
          println("- Removed patient: " + careContactId)
          activePatients -= careContactId
          publishOnAkka(header, api.RemovedPatientReminder(careContactId))
        }
      }
    }
  }

  def getTimePrintString(milliSec: String): String = {
    val minutesDiff = ((milliSec.toLong / (1000*60)) % 60)
    val hoursDiff = ((milliSec.toLong / (1000*60*60)) % 24)
    val daysDiff = milliSec.toLong / (1000*60*60*24)
    return daysDiff + " d " + hoursDiff + " h " + minutesDiff + " m "
  }

  def appendNewPatientData(careContactId: String, patientData: Map[String, String]): Map[String, String] = {
    var updatedPatientData: Map[String, String] = activePatients(careContactId).patientData
    patientData.foreach{ d =>
      updatedPatientData += d._1 -> d._2
    }
    return updatedPatientData
  }

  def appendNewEvents(careContactId: String, newEvents: List[Map[String, String]]): List[Map[String, String]] = {
    var eventsBuffer = new ListBuffer[Map[String,String]]()
    activePatients(careContactId).events.foreach{ e =>
      eventsBuffer += e
    }
    newEvents.foreach{ e =>
      eventsBuffer += e
    }
    return eventsBuffer.toList
  }

  def isFinished(timeToFinished: String): Boolean = {
    if (timeToFinished == "-1") {
      return false
    }
    return true
  }

  def appendFieldsToPatientData(patientData: Map[String, String], events: List[Map[String, String]]): Map[String, String] = {
    val latestEvent = getLatestEvent(events)
    var tmp: Map[String, String] = Map()
    patientData.foreach{ p =>
      tmp += p._1 -> p._2
    }
    tmp += ("LatestEventTitle" -> latestEvent("Title"))
    tmp += ("LatestEventTimeDiff" -> latestEvent("TimeDiff"))
    return tmp
  }

  def getLatestEvent(events: List[Map[String, String]]): Map[String, String] = {
    var timeDiff: Long = Long.MaxValue
    var title = ""
    // Get the latest event title and its time diff
    events.foreach{ e =>
      val startOfEventString = e("Start")
      var formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      val startOfEvent = formatter.parse(startOfEventString.replaceAll("Z$", "+0000"))
      val nowString = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").format(new Date())
      val now = new SimpleDateFormat("yyyy-MM-dd'T'hh:MM:ss'Z'").parse(nowString)
      val diff: Long = now.getTime() - startOfEvent.getTime() // returns diff in millisec
      if (diff < timeDiff) {
        timeDiff = diff
        if (e("Title") == "" || e("Title") == " ") {
          title = "> ej angett <"
        } else {
          title = e("Title")
        }
      }
    }
    return Map("Title" -> title, "TimeDiff" -> timeDiff.toString)
  }


  def publishOnAkka(header: SPHeader, body: api.PatientReminderEvent) {
    val toSend = PatientReminderComm.makeMess(header, body)
    toSend match {
      case Success(v) =>
        mediator ! Publish("patient-reminder-widget-topic", v) // Publishes on bus for widget to receive
      case Failure(e) =>
        println("Failed")
    }
  }

  }

  object PatientReminderDevice {
    def props = Props(classOf[PatientReminderDevice])
  }
