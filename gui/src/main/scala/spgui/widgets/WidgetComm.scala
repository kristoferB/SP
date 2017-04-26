package spgui.widgets

package API_Patient {
  sealed trait PatientProperty
  case class Priority(color: String, timestamp: String) extends PatientProperty
  case class Attended(attended: Boolean, doctorId: String, timestamp: String) extends PatientProperty
  case class Location(roomNr: String, timestamp: String) extends PatientProperty
  case class Team(team: String, clinic: String, timestamp: String) extends PatientProperty
  case class Examination(isOnExam: Boolean, timestamp: String) extends PatientProperty
  case class LatestEvent(latestEvent: String, timeDiff: Long, timestamp: String) extends PatientProperty
  case class ArrivalTime(timeDiff: String, timestamp: String) extends PatientProperty
  case class FinishedStillPresent(finishedStillPresent: Boolean, timestamp: String) extends PatientProperty
  case class Finished() extends PatientProperty

  case class Patient(
    var careContactId: String,
    var priority: Priority,
    var attended: Attended,
    var location: Location,
    var team: Team,
    var examination: Examination,
    var latestEvent: LatestEvent,
    var arrivalTime: ArrivalTime,
    var finishedStillPresent: FinishedStillPresent)
}

package API_PatientEvent {
  import spgui.widgets.{API_Patient => api}

  sealed trait Event

  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent with Event
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent with Event
  case class RemovedPatient(careContactId: String, timestamp: String) extends PatientEvent with Event

  sealed trait StateEvent
  case class GetState() extends StateEvent with Event
  case class State(patients: Map[String, api.Patient]) extends StateEvent with Event

  case class Tick() extends StateEvent with Event

  object attributes {
    val service = "patientCardsService"
  }
}