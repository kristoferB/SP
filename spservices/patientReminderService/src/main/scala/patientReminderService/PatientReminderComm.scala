package sp.patientreminderservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import scala.util.Try

package API_PatientEvent {

  // Messages I can receive (from elvisDataHandlerService)
  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent
  case class RemovedPatient(careContactId: String) extends PatientEvent

  // Messages I can send to my widget
  sealed trait PatientReminderEvent
  case class NewPatientReminder(careContactId: String, patientData: Map[String, String]) extends PatientReminderEvent
  case class RemovedPatientReminder(careContactId: String) extends PatientReminderEvent

  object attributes {
    val service = "patientReminderService"
  }
}

import sp.patientreminderservice.{API_PatientEvent => api}

object PatientReminderComm {

  def extractPatientEvent(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[api.PatientEvent]
  } yield (h, b)

  def makeMess(h: SPHeader, b: api.PatientReminderEvent) = SPMessage.makeJson[SPHeader, api.PatientReminderEvent](h, b)

}
