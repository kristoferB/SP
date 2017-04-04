package sp.triagediagramservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success, Failure }

import sp.triagediagramservice.{API_PatientEvent => api}

class TriageDiagramDevice extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("patient-event-topic", self)

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
          publishOnAkka(header, api.NewPatient(careContactId, patientData, events))
        }
        case api.DiffPatient(careContactId, patientData, newEvents, removedEvents) => {
          println("Diff patient: " + careContactId + ", Prio: " + patientData("Priority"))
          publishOnAkka(header, api.DiffPatient(careContactId, patientData, newEvents, removedEvents))
        }
        case api.RemovedPatient(careContactId) => {
          println("- Removed patient: " + careContactId)
          publishOnAkka(header, api.RemovedPatient(careContactId))
        }
      }
    }
  }

  def publishOnAkka(header: SPHeader, body: api.PatientEvent) {
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
