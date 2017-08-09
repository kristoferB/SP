package sp.gPubSub

import sp.domain._
import sp.domain.Logic._
import scala.util._
import scala.util.Try
import com.github.nscala_time.time.Imports._

// Internally

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

  case class ElvisEvent(CareEventId: Int,
                        Category: String,
                        End: String,
                        Start: String,
                        Title: String,
                        Type: String,
                        Value: String,
                        VisitId: Int)

  case class EricaEvent(CareContactId: Int,
                        Category: String,
                        End: String,
                        Start: String,
                        Title: String,
                        Type: String,
                        Value: String,
                        VisitId: Int,
                        TimeEvent: String)

  case class PatientDiff(updates: Map[String, SPValue], newEvents: List[ElvisEvent], removedEvents: List[ElvisEvent])
  case class NewPatient(timestamp: String, patient: ElvisPatient)
  case class RemovedPatient(timestamp: String, patient: ElvisPatient)
  case class SnapShot(patients: List[ElvisPatient])
}


object API_PatientEvent {
  import datahandler.API_Patient

  sealed trait Event

  case class GetState() extends Event
  case class State(patients: Map[String, API_Patient.Patient]) extends Event

  case class Tick() extends Event

  case class ElvisDataFlowing(dataFlowing: Boolean) extends Event

  // Messages I can send
  //sealed trait ElvisEvent
  //case class ElvisData(events: List[api.EricaEvent]) extends ElvisEvent

  object attributes {
    val service = "gPubSubService"
  }

  object Event {
    implicit lazy val fExampleServiceEvent: JSFormat[Event] = deriveFormatISA[Event]
  }
}

