package spgui.widgets

import sp.domain._
import Logic._
import play.api.libs.json._
import spgui.communication.BackendCommunication

import scala.util.Try




object EricaLogic {

  val dummyPatient = API_Patient.Patient(
    "4502085",
    API_Patient.Priority("NotTriaged", "2017-02-01T15:49:19Z"),
    API_Patient.Attended(true, "sarli29", "2017-02-01T15:58:33Z"),
    API_Patient.Location("52", "2017-02-01T15:58:33Z"),
    API_Patient.Team("GUL", "NAKME", "B", "2017-02-01T15:58:33Z"),
    API_Patient.Examination(false, "2017-02-01T15:58:33Z"),
    API_Patient.LatestEvent("OmsKoord", -1, false, "2017-02-01T15:58:33Z"),
    API_Patient.Plan(false, "2017-02-01T15:58:33Z"),
    API_Patient.ArrivalTime("", "2017-02-01T10:01:38Z"),
    API_Patient.Debugging("NAKKK","B","B23"),
    API_Patient.Finished(false, false, "2017-02-01T10:01:38Z")
  )

}

object API_Patient {
  sealed trait PatientProperty
  case class Priority(color: String, timestamp: String) extends PatientProperty
  case class Attended(attended: Boolean, doctorId: String, timestamp: String) extends PatientProperty
  case class Location(roomNr: String, timestamp: String) extends PatientProperty
  case class Team(team: String, clinic: String, reasonForVisit: String, timestamp: String) extends PatientProperty
  case class Examination(isOnExam: Boolean, timestamp: String) extends PatientProperty
  case class LatestEvent(latestEvent: String, timeDiff: Long, needsAttention: Boolean, timestamp: String) extends PatientProperty
  case class Plan(hasPlan: Boolean, timestamp: String) extends PatientProperty
  case class ArrivalTime(timeDiff: String, timestamp: String) extends PatientProperty
  case class Finished(finished: Boolean, finishedStillPresent: Boolean, timestamp: String) extends PatientProperty
  case class Debugging(clinic: String, reasonForVisit: String, location: String) extends PatientProperty
  case class Removed(timestamp: String) extends PatientProperty
  case object Undefined extends PatientProperty

  case class Patient(
                      var careContactId: String,
                      var priority: Priority,
                      var attended: Attended,
                      var location: Location,
                      var team: Team,
                      var examination: Examination,
                      var latestEvent: LatestEvent,
                      var plan: Plan,
                      var arrivalTime: ArrivalTime,
                      var debugging: Debugging,
                      var finished: Finished
                    )

  object Patient {
    implicit val fapipat1: JSFormat[Priority] = Json.format[Priority]
    implicit val fapipat2: JSFormat[Attended] = Json.format[Attended]
    implicit val fapipat3: JSFormat[Location] = Json.format[Location]
    implicit val fapipat4: JSFormat[Team] = Json.format[Team]
    implicit val fapipat5: JSFormat[Examination] = Json.format[Examination]
    implicit val fapipat6: JSFormat[LatestEvent] = Json.format[LatestEvent]
    implicit val fapipat7: JSFormat[Plan] = Json.format[Plan]
    implicit val fapipat8: JSFormat[ArrivalTime] = Json.format[ArrivalTime]
    implicit val fapipat9: JSFormat[Finished] = Json.format[Finished]
    implicit val fapipat10: JSFormat[Debugging] = Json.format[Debugging]
    implicit val fapipat11: JSFormat[Removed] = Json.format[Removed]

    implicit val fPatient: JSFormat[Patient] = deriveFormatSimple[Patient]
  }

}

object API_PatientEvent {

  sealed trait Event

  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends Event
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends Event
  case class RemovedPatient(careContactId: String, timestamp: String) extends Event

  case class GetState() extends Event
  case class State(patients: Map[String, API_Patient.Patient]) extends Event

  case class Tick() extends Event

  case class ElvisDataFlowing(dataFlowing: Boolean) extends Event

  object attributes {
    val service = "widgetService"
  }

  object Event {
    implicit val fEvent: JSFormat[Event] = deriveFormatISA[Event]
  }
}


object ToAndFrom {
  def eventBody(mess: SPMessage): Try[API_PatientEvent.Event] = mess.getBodyAs[API_PatientEvent.Event]

  def make(h: SPHeader, b: API_PatientEvent.Event): SPMessage = SPMessage.make(h, b)
}
