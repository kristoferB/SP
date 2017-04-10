package sp.statusdiagramservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success, Failure }

import scala.collection.mutable.ListBuffer

import sp.statusdiagramservice.{API_PatientEvent => api}

class StatusDiagramDevice extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("patient-event-topic", self)

  case class Patient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]])

  var activePatients: Map[String, Patient] = Map()

  def receive = {
    case mess @ _ if {log.debug(s"StatusDiagramService MESSAGE: $mess from $sender"); false} => Unit

    case x: String => handleRequests(x)

  }

  def handleRequests(x: String): Unit = {
    val mess = SPMessage.fromJson(x)
    matchRequests(mess)
  }

  def matchRequests(mess: Try[SPMessage]) = {
    val header = SPHeader(from = "statusDiagramService")
    StatusDiagramComm.extractPatientEvent(mess) map { case (h, b) =>
      b match {
        case api.NewPatient(careContactId, patientData, events) => {
          activePatients += careContactId -> Patient(careContactId, patientData, events)
          println("+ New patient: " + careContactId + ", TimeToFinished: " + patientData("TimeToFinished") + ", IsAttended: " + isAttended(events))
          publishOnAkka(header, getStatusApi(patientData, events, true)) // Send mess to widget about incrementing one of the status counters
        }
        case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
          publishOnAkka(header, getStatusApi(activePatients(careContactId).patientData, activePatients(careContactId).events, false)) // Send mess to widget about decrementing one of the status counters
          val updatedPatientData = appendNewPatientData(careContactId, patientDataDiff)
          val updatedEvents = appendNewEvents(careContactId, newEvents)
          println("Diff patient: " + careContactId + getDiffPrint(patientDataDiff, activePatients(careContactId).patientData))
          activePatients += careContactId -> Patient(careContactId, updatedPatientData, updatedEvents)
          publishOnAkka(header, getStatusApi(updatedPatientData, updatedEvents, true)) // Send mess to widget about incrementing one of the status counters
        }
        case api.RemovedPatient(careContactId) => {
          publishOnAkka(header, getStatusApi(activePatients(careContactId).patientData, activePatients(careContactId).events, false)) // Send mess to widget about decrementing one of the status counters
          activePatients -= careContactId
          println("- Removed patient: " + careContactId)
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

  def getStatusApi(patientData: Map[String, String], events: List[Map[String, String]], toAdd: Boolean): api.StatusEvent = {
    if (patientData("TimeToFinished") != "-1") {
      return api.Finished(toAdd)
    }
    events.foreach{ e =>
      val title = e("Title")
      if (title == "Läkare" || title == "läkare" || title == "L\\u00c4KARE" || title == "L\u00c4KARE" || title == "LÄKARE") {
        return api.Attended(toAdd)
      }
    }
    return api.Unattended(toAdd)
  }

  def isAttended(events: List[Map[String, String]]): Boolean = {
    events.foreach{ e =>
      val title = e("Title")
      if (title == "Läkare" || title == "läkare" || title == "L\\u00c4KARE" || title == "L\u00c4KARE" || title == "LÄKARE") {
        return true
      }
    }
    return false
  }

  def publishOnAkka(header: SPHeader, body: api.StatusEvent) {
    val toSend = StatusDiagramComm.makeMess(header, body)
    toSend match {
      case Success(v) =>
        mediator ! Publish("status-diagram-widget-topic", v) // Publishes on bus for widget to receive
      case Failure(e) =>
        println("Failed")
    }
  }

  }

  object StatusDiagramDevice {
    def props = Props(classOf[StatusDiagramDevice])
  }
