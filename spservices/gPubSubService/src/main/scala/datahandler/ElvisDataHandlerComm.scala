package datahandler

import sp.domain._
import sp.domain.Logic._
import scala.util._

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
  case class Undefined() extends PatientProperty

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
}

object API_Data {
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

  case class EricaEvent(CareContactId: Int,
                      Category: String,
                      End: String,
                      Start: String,
                      Title: String,
                      Type: String,
                      Value: String,
                      VisitId: Int,
                      TimeEvent: String)
}

object API_PatientEvent {
  import datahandler.{API_Data => api}
  import datahandler.{API_Patient => patientApi}

  sealed trait Event

  sealed trait StateEvent
  case class GetState() extends StateEvent with Event
  case class State(patients: Map[String, patientApi.Patient]) extends StateEvent with Event

  case class Tick() extends StateEvent with Event

  case class ElvisDataFlowing(dataFlowing: Boolean) extends StateEvent with Event

  // Messages I can receive
  sealed trait ElvisEvent
  case class ElvisData(events: List[api.EricaEvent]) extends ElvisEvent

  object attributes {
    val service = "elvisDataHandlerService"
  }
}

import sp.gPubSub.{API_PatientEvent => api}

object ElvisDataHandlerComm {
/**
  def extractElvisEvent(mess: Try[SPMessage]) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader]
      b <- m.getBodyAs[api.ElvisEvent]
    } yield (h, b)*/


  def makeMess(h: SPHeader, b: api.Event) = SPMessage.makeJson[SPHeader, api.Event](h, b)

}
