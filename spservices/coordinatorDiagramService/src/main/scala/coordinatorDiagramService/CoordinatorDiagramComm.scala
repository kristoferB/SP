package sp.coordinatordiagramservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import scala.util.Try

package API_PatientEvent {

  // Messages I can receive (from elvisDataHandler)
  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent
  case class RemovedPatient(careContactId: String, timestamp: String) extends PatientEvent

  // Messages I can send to my widget
  sealed trait PatientProperty

  sealed trait PriorityEvent extends PatientProperty
  case class NotTriaged(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Green(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Yellow(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Orange(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Red(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent

  sealed trait FinishedEvent
  case class Finished(careContactId: String, timestamp: String) extends PatientProperty with FinishedEvent

  case class Undefined(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent with FinishedEvent

  object attributes {
    val service = "coordinatorDiagramService"
  }
}

import sp.coordinatordiagramservice.{API_PatientEvent => api}

object CoordinatorDiagramComm {

  def extractPatientEvent(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[api.PatientEvent]
  } yield (h, b)

  def makeMess(h: SPHeader, b: api.PatientProperty) = SPMessage.makeJson[SPHeader, api.PatientProperty](h, b)

}
