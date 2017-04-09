package sp.triagediagramservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success, Failure }

import scala.collection.mutable.ListBuffer

import sp.triagediagramservice.{API_PatientEvent => api}

class TriageDiagramDevice extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("patient-event-topic", self)

  case class Patient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]])
  case class Triage(colorMap: Map[String, Int])

  var activePatients: Map[String, Patient] = Map()

  def receive = {
    case mess @ _ if {log.debug(s"TriageDiagramService MESSAGE: $mess from $sender"); false} => Unit

    case x: String => handleRequests(x)

  }

  def handleRequests(x: String): Unit = {
    val mess = SPMessage.fromJson(x)
    matchRequests(mess)
  }

  def matchRequests(mess: Try[SPMessage]) = {
    val header = SPHeader(from = "triageDiagramService")
    TriageDiagramComm.extractPatientEvent(mess) map { case (h, b) =>
      b match {
        case api.NewPatient(careContactId, patientData, events) => {
          println("+ New patient: " + careContactId + ", Prio: " + patientData("Priority"))
          activePatients += careContactId -> Patient(careContactId, patientData, events)
          publishOnAkka(header, getTriageApi(patientData("Priority"), true)) // Send mess to widget about incrementing one of the triage counters
        }
        case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
          val updatedPatientData = appendNewPatientData(careContactId, patientDataDiff)
          val oldTriage = activePatients(careContactId).patientData("Priority")
          val newTriage = updatedPatientData("Priority")
          println("Diff patient: " + careContactId + getDiffPrint(patientDataDiff, activePatients(careContactId).patientData))
          val updatedEvents = appendNewEvents(careContactId, newEvents)
          activePatients += careContactId -> Patient(careContactId, updatedPatientData, updatedEvents)
          publishOnAkka(header, getTriageApi(oldTriage, false)) // Send mess to widget about decrementing one of the triage counters
          publishOnAkka(header, getTriageApi(newTriage, true)) // Send mess to widget about incrementing one of the triage counters
        }
        case api.RemovedPatient(careContactId) => {
          println("- Removed patient: " + careContactId)
          publishOnAkka(header, getTriageApi(activePatients(careContactId).patientData("Priority"), false)) // Send mess to widget about decrementing one of the triage counters
          activePatients -= careContactId
        }
      }
    }
  }

  def getDiffPrint(newData: Map[String, String], oldData: Map[String, String]): String = {
    if (newData("Location") != "") {
      return ", Changed fields: Location: [from: " + oldData("Location") + ", to: " + newData("Location") + "] "
    }
    if (newData("Team") != "") {
      return ", Changed fields: Team: [from: " + oldData("Team") + ", to: " + newData("Team") + "] "
    }
    if (newData("ReasonForVisit") != "") {
      return ", Changed fields: ReasonForVisit: [from: " + oldData("ReasonForVisit") + ", to: " + newData("ReasonForVisit") + "] "
    }
    if (newData("DepartmentComment") != "") {
      return ", Changed fields: DepartmentComment: [from: " + oldData("DepartmentComment") + ", to: " + newData("DepartmentComment") + "] "
    }
    return "Changed fields: NONE"
  }

  def getTriageApi(triage: String, toAdd: Boolean): api.TriageEvent = {
    triage match {
      case "Grön" => api.Green(toAdd)
      case "Gul" => api.Yellow(toAdd)
      case "Orange" => api.Orange(toAdd)
      case "Röd" => api.Red(toAdd)
      case _ => api.Undefined(toAdd)
    }
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

  def publishOnAkka(header: SPHeader, body: api.TriageEvent) {
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
