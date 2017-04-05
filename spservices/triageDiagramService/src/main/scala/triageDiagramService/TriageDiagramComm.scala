package sp.triagediagramservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import scala.util.Try

package API_PatientEvent {

  // Messages I can receive (from elvisDataHandler)
  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent
  case class RemovedPatient(careContactId: String) extends PatientEvent

  // Messages I can send (to my widget)
  sealed trait TriageEvent
  case class Undefined(toAdd: Boolean) extends TriageEvent
  case class Green(toAdd: Boolean) extends TriageEvent
  case class Yellow(toAdd: Boolean) extends TriageEvent
  case class Orange(toAdd: Boolean) extends TriageEvent
  case class Red(toAdd: Boolean) extends TriageEvent

  object attributes {
    val service = "triageDiagramService"
  }
}

import sp.triagediagramservice.{API_PatientEvent => api}

object TriageDiagramComm {

  def extractPatientEvent(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[api.PatientEvent]
  } yield (h, b)

  def makeMess(h: SPHeader, b: api.TriageEvent) = SPMessage.makeJson[SPHeader, api.TriageEvent](h, b)

}
