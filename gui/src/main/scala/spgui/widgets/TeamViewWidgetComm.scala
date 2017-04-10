package spgui.widgets

package API_PatientEvent {

  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent
  case class RemovedPatient(careContactId: String) extends PatientEvent

  sealed trait PatientReminderEvent
  case class NewPatientReminder(careContactId: String, patientData: Map[String, String]) extends PatientReminderEvent
  case class RemovedPatientReminder(careContactId: String) extends PatientReminderEvent

  sealed trait TriageEvent
  case class PingUndefined(toAdd: Boolean) extends TriageEvent
  case class PingGreen(toAdd: Boolean) extends TriageEvent
  case class PingYellow(toAdd: Boolean) extends TriageEvent
  case class PingOrange(toAdd: Boolean) extends TriageEvent
  case class PingRed(toAdd: Boolean) extends TriageEvent

  sealed trait StatusEvent
  case class PingUnattended(toAdd: Boolean) extends StatusEvent
  case class PingAttended(toAdd: Boolean) extends StatusEvent
  case class PingFinished(toAdd: Boolean) extends StatusEvent

  /*
    Used by PatientCardsService
  **/
  sealed trait PatientProperty

  sealed trait PriorityEvent extends PatientProperty
  case class NotTriaged(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Green(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Yellow(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Orange(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Red(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent

  sealed trait AttendedEvent
  case class Attended(careContactId: String, attended: Boolean, attendantId: String, timestamp: String) extends PatientProperty with AttendedEvent

  sealed trait LocationEvent
  case class RoomNr(careContactId: String, roomNr: String) extends PatientProperty with LocationEvent

  sealed trait TeamEvent
  case class Team(careContactId: String, team: String, klinik: String) extends PatientProperty with TeamEvent

  sealed trait LatestEventEvent
  case class LatestEvent(careContactId: String, latestEvent: String, timestamp: String) extends PatientProperty with LatestEventEvent

  sealed trait ArrivalTimeEvent
  case class ArrivalTime(careContactId: String, timestamp: String) extends PatientProperty with ArrivalTimeEvent

  sealed trait FinishedEvent
  case class Finished(careContactId: String) extends PatientProperty with FinishedEvent

  case class Undefined(careContactId: String) extends PatientProperty with PriorityEvent with AttendedEvent with LocationEvent with TeamEvent with LatestEventEvent with ArrivalTimeEvent with FinishedEvent


  object attributes {
    val service = "patientCardsService"
  }
}
