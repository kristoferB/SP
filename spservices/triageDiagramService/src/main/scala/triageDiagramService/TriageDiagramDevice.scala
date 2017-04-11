package sp.triagediagramservice

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

import sp.triagediagramservice.{API_PatientEvent => api}

class TriageDiagramDevice extends Actor with ActorLogging {
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
    case mess @ _ if {log.debug(s"TriageDiagramService MESSAGE: $mess from $sender"); false} => Unit

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
    val header = SPHeader(from = "triageDiagramService")
    TriageDiagramComm.extractPatientEvent(mess) map { case (h, b) =>
      b match {
        case api.NewPatient(careContactId, patientData, events) => {
          println("+ New patient: " + careContactId + ", Prio: " + patientData("Priority"))
          publishOnAkka(header, updatePriority(careContactId, patientData("timestamp"), patientData("Priority")))
        }
        case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
          println("DIFF: new events: " + newEvents + ", removed events: " + removedEvents)
          val latestPrioEvent = getLatestPrioEvent(careContactId, newEvents)
          if (latestPrioEvent != api.Undefined(careContactId, "0000-00-00T00:00:00.000Z")) {
            publishOnAkka(header, latestPrioEvent)
          }
        }
        case api.RemovedPatient(careContactId, timestamp) => {
          println("- Removed patient: " + careContactId)
          publishOnAkka(header, api.Finished(careContactId, timestamp))
        }
      }
    }
  }

  /**
  Identifies the latest event considering triage and returns corresponding PriorityEvent.
  OBS: revidera
  */
  def getLatestPrioEvent(careContactId: String, events: List[Map[String, String]]): api.PatientProperty = {
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
    return api.Undefined(careContactId, "0000-00-00T00:00:00.000Z")
  }

  /**
  Discerns priority and returns corresponding PriorityEvent-type
  */
  def updatePriority(careContactId: String, timestamp: String, priority: String): api.PriorityEvent = {
    priority match {
      case "Grön" => api.Green(careContactId, timestamp)
      case "Gul" =>  api.Yellow(careContactId, timestamp)
      case "Orange" => api.Orange(careContactId, timestamp)
      case "Röd" => api.Red(careContactId, timestamp)
      case _ => api.NotTriaged(careContactId, timestamp)
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
  Publishes a SPMessage on bus with header and body
  */
  def publishOnAkka(header: SPHeader, body: api.PatientProperty) {
    val toSend = TriageDiagramComm.makeMess(header, body)
    toSend match {
      case Success(v) =>
        mediator ! Publish("triage-diagram-widget-topic", v) // Publishes on bus for widget to receive
      case Failure(e) =>
        println("Failed")
    }
  }

  }

  object TriageDiagramDevice {
    def props = Props(classOf[TriageDiagramDevice])
  }
