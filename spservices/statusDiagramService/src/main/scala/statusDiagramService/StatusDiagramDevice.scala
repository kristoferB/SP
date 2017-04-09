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
  var activePatientCards: Map[String, Patient] = Map()

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
          activePatientCards += careContactId -> Patient(careContactId, patientData, events)
          println("+ New patient: " + careContactId + ", TimeToFinished: " + patientData("TimeToFinished") + ", IsAttended: " + isAttended(events))
          publishOnAkka(header, getStatusApi(patientData, events, true))
        }
        case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
          var modPatientData = activePatientCards(careContactId).patientData
          publishOnAkka(header, getStatusApi(modPatientData, activePatientCards(careContactId).events, false))
          patientDataDiff.foreach{ d =>
            modPatientData += d._1 -> d._2
          }
          var eventsBuffer = new ListBuffer[Map[String,String]]()
          activePatientCards(careContactId).events.foreach{ e =>
            eventsBuffer += e
          }
          newEvents.foreach{ e =>
            eventsBuffer += e
          }
          activePatientCards += careContactId -> Patient(careContactId, modPatientData, eventsBuffer.toList)
          println("Diff patient: " + careContactId + ", TimeToFinished: " + modPatientData("TimeToFinished") + ", IsAttended: " + isAttended(eventsBuffer.toList))
          publishOnAkka(header, getStatusApi(modPatientData, eventsBuffer.toList, true))
        }
        case api.RemovedPatient(careContactId) => {
          publishOnAkka(header, getStatusApi(activePatientCards(careContactId).patientData, activePatientCards(careContactId).events, false))
          activePatientCards -= careContactId
          println("- Removed patient: " + careContactId)
        }
      }
    }
  }

  def getStatusApi(patientData: Map[String, String], events: List[Map[String, String]], toAdd: Boolean): api.StatusEvent = {
    if (patientData("TimeToFinished") != "-1") {
      return api.Finished(toAdd)
    }
    events.foreach{ e =>
      val title = e("Title")
      if (title == "Läkare" || title == "läkare" || title == "L\\u00c4KARE" || title == "L\u00c4KARE") {
        return api.Attended(toAdd)
      }
    }
    return api.Unattended(toAdd)
  }

  def isAttended(events: List[Map[String, String]]): Boolean = {
    events.foreach{ e =>
      val title = e("Title")
      if (title == "Läkare" || title == "läkare" || title == "L\\u00c4KARE" || title == "L\u00c4KARE") {
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
