package sp.gPubSub

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

object GPubSubComm {

}
