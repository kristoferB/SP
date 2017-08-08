package sp.gPubSub

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._
import scala.util._
import scala.util.Try
import com.github.nscala_time.time.Imports._
import org.json4s._

// Internally

package API_Data {
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

case class PatientDiff(updates: Map[String, JValue], newEvents: List[ElvisEvent], removedEvents: List[ElvisEvent])
case class NewPatient(timestamp: String, patient: ElvisPatient)
case class RemovedPatient(timestamp: String, patient: ElvisPatient)
case class SnapShot(patients: List[ElvisPatient])
}


package API_PatientEvent {
  //import sp.gPubSub.{API_Data => api}
  import datahandler.{API_Data => api}
  import datahandler.{API_Patient => patientApi}

  sealed trait Event

  sealed trait StateEvent
  case class GetState() extends StateEvent with Event
  case class State(patients: Map[String, patientApi.Patient]) extends StateEvent with Event

  case class Tick() extends StateEvent with Event

  case class ElvisDataFlowing(dataFlowing: Boolean) extends StateEvent with Event

  // Messages I can send
  //sealed trait ElvisEvent
  //case class ElvisData(events: List[api.EricaEvent]) extends ElvisEvent

  object attributes {
    val service = "gPubSubService"
  }
}

import sp.gPubSub.{API_PatientEvent => api}

object GPubSubComm {

}
