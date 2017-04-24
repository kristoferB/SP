package spgui.widgets

package API_PatientEvent {

  sealed trait PatientProperty

  case class Tick() extends PatientProperty

  sealed trait PriorityEvent extends PatientProperty
  case class NotTriaged(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Blue(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Green(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Yellow(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Orange(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent
  case class Red(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent

  sealed trait AttendedEvent
  case class Attended(careContactId: String, timestamp: String, attended: Boolean, doctorId: String) extends PatientProperty with AttendedEvent

  sealed trait ExaminationEvent
  case class Examination(careContactId: String, timestamp: String, isOnExam: Boolean) extends PatientProperty with ExaminationEvent

  sealed trait LocationEvent
  case class RoomNr(careContactId: String, timestamp: String, roomNr: String) extends PatientProperty with LocationEvent

  sealed trait TeamEvent
  case class Team(careContactId: String, timestamp: String, team: String, klinik: String) extends PatientProperty with TeamEvent

  sealed trait ReasonForVisitEvent
  case class ReasonForVisit(careContactId: String, timestamp: String, reason: String) extends PatientProperty with ReasonForVisitEvent

  sealed trait LatestEventEvent
  case class LatestEvent(careContactId: String, timestamp: String, latestEvent: String, timeDiff: Long) extends PatientProperty with LatestEventEvent

  sealed trait ArrivalTimeEvent
  case class ArrivalTime(careContactId: String, timestamp: String, timeDiff: String) extends PatientProperty with ArrivalTimeEvent

  sealed trait FinishedEvent
  case class FinishedStillPresent(careContactId: String, timestamp: String) extends PatientProperty with FinishedEvent
  case class Finished(careContactId: String, timestamp: String) extends PatientProperty with FinishedEvent

  case class Undefined(careContactId: String, timestamp: String) extends PatientProperty with PriorityEvent with AttendedEvent with LocationEvent with TeamEvent with LatestEventEvent with ArrivalTimeEvent with FinishedEvent

  object attributes {
    val service = "patientCardsService"
  }
}
