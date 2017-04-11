package sp.patientcardsservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import scala.util.Try

package API_PatientEvent {
  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent
  case class RemovedPatient(careContactId: String, timestamp: String) extends PatientEvent

  sealed trait PatientProperty

  sealed trait PriorityEvent extends PatientProperty
  case class NotTriaged(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Green(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Yellow(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Orange(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Red(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent

  sealed trait AttendedEvent
  case class Attended(careContactId: String, timestamp: String, attended: Boolean, doctorId: String) extends PatientProperty with AttendedEvent

  sealed trait LocationEvent
  case class RoomNr(careContactId: String, timestamp: String, roomNr: String) extends PatientProperty with LocationEvent

  sealed trait TeamEvent
  case class Team(careContactId: String, timestamp: String, team: String, klinik: String) extends PatientProperty with TeamEvent

  sealed trait LatestEventEvent
  case class LatestEvent(careContactId: String, timestamp: String, latestEvent: String) extends PatientProperty with LatestEventEvent

  sealed trait ArrivalTimeEvent
  case class ArrivalTime(careContactId: String, timestamp: String) extends PatientProperty with ArrivalTimeEvent

  sealed trait FinishedEvent
  case class Finished(careContactId: String, timestamp: String) extends PatientProperty with FinishedEvent

  case class Undefined(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent with AttendedEvent with LocationEvent with TeamEvent with LatestEventEvent with ArrivalTimeEvent with FinishedEvent

  object attributes {
    val service = "patientCardsService"
  }
}


import sp.patientcardsservice.{API_PatientEvent => api}

object PatientCardsComm {

  def extractPatientEvent(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[api.PatientEvent]
  } yield (h, b)

  def makeMess(h: SPHeader, b: api.PatientProperty) = SPMessage.makeJson[SPHeader, api.PatientProperty](h, b)

}
