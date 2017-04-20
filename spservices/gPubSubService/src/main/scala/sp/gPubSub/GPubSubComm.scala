package sp.gPubSub

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._
import scala.util._
import scala.util.Try
import com.github.nscala_time.time.Imports._
import org.json4s._

case class ElvisPatient(CareContactId: Int,
                        CareContactRegistrationTime: DateTime,
                        DepartmentComment: String,
                        Events: List[ElvisEvent],
                        Location: String,
                        PatientId: Int,
                        ReasonForVisit: String,
                        Team: String,
                        VisitId: Int,
                        VisitRegistrationTime: DateTime)

case class ElvisEvent(CareEventId: Int,
                      Category: String,
                      End: DateTime,
                      Start: DateTime,
                      Title: String,
                      Type: String,
                      Value: String,
                      VisitId: Int)

case class PatientDiff(updates: Map[String, JValue], newEvents: List[ElvisEvent], removedEvents: List[ElvisEvent])
case class NewPatient(timestamp: DateTime, patient: ElvisPatient)
case class RemovedPatient(timestamp: DateTime, patient: ElvisPatient)
case class SnapShot(patients: List[ElvisPatient])

package API_PatientEvent {
  // Messages I can send
  sealed trait ElvisEvent
  case class ElvisData(data: String) extends ElvisEvent

  object attributes {
    val service = "gPubSubService"
  }
}

import sp.gPubSub.{API_PatientEvent => api}

object GPubSubComm {

  //def makeMess(h: SPHeader, b: api.ElvisEvent) = SPMessage.makeJson[SPHeader, api.ElvisEvent](h, b)

}
