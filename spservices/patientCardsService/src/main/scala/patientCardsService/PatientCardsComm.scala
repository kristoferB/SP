package sp.patientcardsservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import scala.util.Try

// package API_PatientEvent {
//
//   // Messages I can send and receive
//   sealed trait PatientEvent
//   case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent
//   case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent
//   case class RemovedPatient(careContactId: String) extends PatientEvent
//
//   object attributes {
//     val service = "patientCardsService"
//   }
// }

package API_PatientEvent {
  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent
  case class RemovedPatient(careContactId: String) extends PatientEvent

  sealed trait TriageEvent
  case class NotTriaged(careContactId: String, timestamp: String) extends TriageEvent
  case class Green(careContactId: String, timestamp: String) extends TriageEvent
  case class Yellow(careContactId: String, timestamp: String) extends TriageEvent
  case class Orange(careContactId: String, timestamp: String) extends TriageEvent
  case class Red(careContactId: String, timestamp: String) extends TriageEvent

  sealed trait StatusEvent
  case class Unattended(careContactId: String, toAdd: Boolean) extends StatusEvent
  case class Attended(careContactId: String, doctorId: String, timestamp: String) extends StatusEvent
  case class Finished(careContactId: String, timestamp: String) extends StatusEvent

  sealed trait LocationEvent
  case class RoomNr(careContactId: String, roomNr: String) extends LocationEvent

  sealed trait TeamEvent
  case class Team(careContactId: String, team: String, klinik: String) extends TeamEvent

  sealed trait LatestEventEvent
  case class LatestEvent(careContactId: String, latestEvent: String, timestamp: String) extends LatestEventEvent

  sealed trait ArrivalTimeEvent
  case class arrivalTime(careContactId: String, arrivalTime: String) extends ArrivalTimeEvent

  case class Undefined(careContactId: String)

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

  def makeMess(h: SPHeader, b: api.PatientEvent) = SPMessage.makeJson[SPHeader, api.PatientEvent](h, b)

}
