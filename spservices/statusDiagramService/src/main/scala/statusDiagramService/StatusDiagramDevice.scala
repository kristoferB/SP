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
          val patientProperties = extractNewPatientProperties(api.NewPatient(careContactId, patientData, events))
          if (!patientProperties.isEmpty) {
            printProperties("NEW PATIENT: PatientProps to send: ", patientProperties)
            for (patientProperty <- patientProperties) {
              publishOnAkka(header, patientProperty)
            }
          }
        }
        case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
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
            }
            if (e("Title") == "Klar") {
              finishedStillPresent = true
              finishedTimestamp = e("Start")
            }
          }
          if (finishedStillPresent) {
            println("DIFF PATIENT: " + careContactId + ": finished still present")
            publishOnAkka(header, api.FinishedStillPresent(careContactId, finishedTimestamp))
          } else {
            if (attended) {
              println("DIFF PATIENT: " + careContactId + ": attended")
              publishOnAkka(header, api.Attended(careContactId, attendedTimestamp, true, doctorId))
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
  Takes a NewPatient and extracts PatientProperties based on patient data and events.
  */
  def getNewPatientProperties(patient: api.NewPatient): List[api.PatientProperty] = {
    return List(updateAttended(patient.careContactId, patient.events))
  }

  /**
  Takes a DiffPatient and extracts PatientProperties based on updates and events.
  */
  def getDiffPatientProperties(patient: api.DiffPatient): List[api.PatientProperty] = {
    var patientPropertyBuffer = new ListBuffer[api.PatientProperty]()
    patient.newEvents.foreach{ e =>
      if (e("Title") == "Läkare") {
        patientPropertyBuffer += api.Attended(patient.careContactId, e("Start"), true, e("Value"))
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
  }

  /**
  Filters out unwanted patient properties.
  */
  def filterDiffPatientProperties(patient: api.DiffPatient, patientProperties: List[api.PatientProperty]): List[api.PatientProperty] = {
    patientProperties
      .filter(_ != (api.Undefined(patient.careContactId, "0000-00-00T00:00:00.000Z")))
  }

  /**
  Checks if patient has been attended by doctor, returns an Attended-type.
  */
  def updateAttended(careContactId: String, events: List[Map[String, String]]): api.Attended = {
    events.foreach{ e =>
      if (e("Title") == "Läkare" || e("Title") == "läkare") {
        return api.Attended(careContactId, e("Start"), true, e("Value"))
      }
    }
    return api.Attended(careContactId, "-1", false, "NA")
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
