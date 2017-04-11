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

  /**
  Receives incoming messages on the AKKA-bus
  */
  def receive = {
    case mess @ _ if {log.debug(s"StatusDiagramService MESSAGE: $mess from $sender"); false} => Unit
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
    val header = SPHeader(from = "statusDiagramService")
    StatusDiagramComm.extractPatientEvent(mess) map { case (h, b) =>
      b match {
        case api.NewPatient(careContactId, patientData, events) => {
          println("+ New patient: " + careContactId + ", TimeToFinished: " + patientData("TimeToFinished") + ", IsAttended: " + isAttended(events))
          publishOnAkka(header, getAttendedApi(careContactId, events))
        }
        case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
          println("Diff patient: " + careContactId)
          var attended: Boolean = false
          var finishedStillPresent: Boolean = false
          var attendedTimestamp = ""
          var doctorId = ""
          var finishedTimestamp = ""
          newEvents.foreach{ e =>
            if (e("Title") == "Läkare") {
              attended = true
              attendedTimestamp = e("Start")
              doctorId = e("Value")
              publishOnAkka(header, api.Attended(careContactId, e("Start"), true, e("Value")))
            }
            if (e("Title") == "Klar") {
              finishedStillPresent = true
              finishedTimestamp = e("Start")
            }
          }
          if (finishedStillPresent) {
            publishOnAkka(header, api.FinishedStillPresent(careContactId, finishedTimestamp))
          } else {
            if (attended) {
              publishOnAkka(header, api.Attended(careContactId, attendedTimestamp, true, doctorId))
            }
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
  Checks if patient has been attended, based on information in events.
  */
  def isAttended(events: List[Map[String, String]]): Boolean = {
    events.foreach{ e =>
      val title = e("Title")
      if (title == "Läkare" || title == "läkare" || title == "L\\u00c4KARE" || title == "L\u00c4KARE" || title == "LÄKARE") {
        return true
      }
    }
    return false
  }

  /**
  Checks if patient has been attended by doctor, returns an Attended-type.
  */
  def getAttendedApi(careContactId: String, events: List[Map[String, String]]): api.Attended = {
    events.foreach{ e =>
      if (e("Title") == "Läkare" || e("Title") == "läkare") {
        return api.Attended(careContactId, e("Start"), true, e("Value"))
      }
    }
    return api.Attended(careContactId, "-1", false, "NA")
  }

  /**
  Publishes a SPMessage on bus with header and body
  */
  def publishOnAkka(header: SPHeader, body: api.PatientProperty) {
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
