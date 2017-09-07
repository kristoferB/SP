package sp.erica {

import sp.domain._
import Logic._
  import play.api.libs.json._

object API_Data {
  case class ElvisPatient(CareContactId: Int,
                          CareContactRegistrationTime: String,
                          DepartmentComment: String,
                          Events: List[ElvisEvent],
                          Location: String,
                          PatientId: Int,
                          ReasonForVisit: String,
                          Team: String,
                          VisitId: Int,
                          VisitRegistrationTime: String)
  object ElvisPatient {
    implicit val fElvisPatient: JSFormat[ElvisPatient] = Json.format[ElvisPatient]
  }

  case class EricaPatient(CareContactId: Int,
                          DepartmentComment: String,
                          Location: String,
                          ReasonForVisit: String,
                          Clinic: String,
                          Priority: String,
                          LatestEvent: String,
                          LatestEventTimeDiff: Long,
                          IsAttended: Boolean,
                          DoctorId: String,
                          NeedsAttention: Boolean,
                          OnExamination: Boolean,
                          HasPlan: Boolean,
                          IsFinished: Boolean,
                          VisitId: Int,
                          VisitRegistrationTime: String)

  object EricaPatient {
    implicit val fEricaPatient: JSFormat[EricaPatient] = Json.format[EricaPatient]
  }

  case class ElvisEvent(CareEventId: Int,
                        Category: String,
                        End: String,
                        Start: String,
                        Title: String,
                        Type: String,
                        Value: String,
                        VisitId: Int)
  object ElvisEvent {
    implicit val fElvisEvent: JSFormat[ElvisEvent] = Json.format[ElvisEvent]
  }

  case class EricaEvent(CareContactId: Int,
                        Category: String,
                        End: String,
                        Start: String,
                        Title: String,
                        Type: String,
                        Value: String,
                        VisitId: Int,
                        TimeEvent: String)
  object EricaEvent {
    implicit val fEricaEvent: JSFormat[EricaEvent] = Json.format[EricaEvent]
  }

  case class PatientDiff(updates: Map[String, SPValue], newEvents: List[ElvisEvent], removedEvents: List[ElvisEvent])
  object PatientDiff {
    implicit val fPatientDiff: JSFormat[PatientDiff] = Json.format[PatientDiff]
  }
  case class NewPatient(timestamp: String, patient: ElvisPatient)
  object NewPatient {
    implicit val fNewPatient: JSFormat[NewPatient] = Json.format[NewPatient]
  }
  case class RemovedPatient(timestamp: String, patient: ElvisPatient)
  object RemovedPatient {
    implicit val fRemovedPatient: JSFormat[RemovedPatient] = Json.format[RemovedPatient]
  }
  case class SnapShot(patients: List[ElvisPatient])
  object SnapShot {
    implicit val fSnapShot: JSFormat[SnapShot] = Json.format[SnapShot]
  }
}

object API_Patient {
  case class Priority(color: String, timestamp: String)
  case class Attended(attended: Boolean, doctorId: String, timestamp: String)
  case class Location(roomNr: String, timestamp: String)
  case class Team(team: String, clinic: String, reasonForVisit: String, timestamp: String)
  case class Examination(isOnExam: Boolean, timestamp: String)
  case class LatestEvent(latestEvent: String, timeDiff: Long, needsAttention: Boolean, timestamp: String)
  case class Plan(hasPlan: Boolean, timestamp: String)
  case class ArrivalTime(timeDiff: String, timestamp: String)
  case class Finished(finished: Boolean, finishedStillPresent: Boolean, timestamp: String)
  case class Debugging(clinic: String, reasonForVisit: String, location: String)
  case class Removed(timestamp: String)

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

    implicit val fPatient: JSFormat[Patient] = Json.format[Patient]
  }

}



object API_PatientEvent {
  val service = "widgetservice"

  sealed trait Event
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends Event
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends Event
  case class RemovedPatient(careContactId: String, timestamp: String) extends Event
  case object GetState extends Event
  case class State(patients: Map[String, API_Patient.Patient]) extends Event
  case object Tick extends Event
  case class ElvisDataFlowing(dataFlowing: Boolean) extends Event

  object Formats {
    implicit val fNewPatient: JSFormat[NewPatient] = Json.format[NewPatient]
    implicit val fDiffPatient: JSFormat[DiffPatient] = Json.format[DiffPatient]
    implicit val fRemovedPatient: JSFormat[RemovedPatient] = Json.format[RemovedPatient]
    implicit val fGetState : JSFormat[GetState.type] = deriveCaseObject[GetState.type]
    implicit val fState: JSFormat[State] = Json.format[State]
    implicit val fTick : JSFormat[Tick.type] = deriveCaseObject[Tick.type]
    implicit val fElvisDataFlowing: JSFormat[ElvisDataFlowing] = Json.format[ElvisDataFlowing]
    def fAPI_PatientEvent: JSFormat[Event] = Json.format[Event]
  }

  object Event {
    implicit lazy val fAPI_PatientEvent: JSFormat[Event] = Formats.fAPI_PatientEvent
  }

}

}

