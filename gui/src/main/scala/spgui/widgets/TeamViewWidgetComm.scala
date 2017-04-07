package spgui.widgets

package API_PatientEvent {

  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent
  case class RemovedPatient(careContactId: String) extends PatientEvent

  sealed trait PatientReminderEvent
  case class AddPatientReminder(careContactId: String, reminderData: Map[String, String]) extends PatientReminderEvent
  case class RemovePatientReminder(careContactId: String) extends PatientReminderEvent

  sealed trait TriageEvent
  case class Undefined(toAdd: Boolean) extends TriageEvent
  case class Green(toAdd: Boolean) extends TriageEvent
  case class Yellow(toAdd: Boolean) extends TriageEvent
  case class Orange(toAdd: Boolean) extends TriageEvent
  case class Red(toAdd: Boolean) extends TriageEvent

  sealed trait StatusEvent
  case class Unattended(toAdd: Boolean) extends StatusEvent
  case class Attended(toAdd: Boolean) extends StatusEvent
  case class Finished(toAdd: Boolean) extends StatusEvent

  object attributes {
    val service = "patientCardsService"
  }
}
