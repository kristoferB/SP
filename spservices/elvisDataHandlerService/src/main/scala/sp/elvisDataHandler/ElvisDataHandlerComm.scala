package sp.elvisdatahandler

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._
import scala.util._

package API_Patient {
  sealed trait PatientProperty
  case class Priority(color: String, timestamp: String) extends PatientProperty
  case class Attended(attended: Boolean, doctorId: String, timestamp: String) extends PatientProperty
  case class Location(roomNr: String, timestamp: String) extends PatientProperty
  case class Team(team: String, clinic: String, timestamp: String) extends PatientProperty
  case class Examination(isOnExam: Boolean, timestamp: String) extends PatientProperty
  case class LatestEvent(latestEvent: String, timeDiff: Long, needsAttention: Boolean, timestamp: String) extends PatientProperty
  case class ArrivalTime(timeDiff: String, timestamp: String) extends PatientProperty
  case class FinishedStillPresent(finishedStillPresent: Boolean, timestamp: String) extends PatientProperty
  case class Finished() extends PatientProperty
  case class Undefined() extends PatientProperty

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
  import sp.elvisdatahandler.{API_Patient => api}

  sealed trait Event

  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent with Event
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent with Event
  case class RemovedPatient(careContactId: String, timestamp: String) extends PatientEvent with Event
  case class Undefined() extends PatientEvent with Event

  sealed trait StateEvent
  case class GetState() extends StateEvent with Event
  case class State(patients: Map[String, api.Patient]) extends StateEvent with Event

  case class Tick() extends StateEvent with Event

/**  // Messages I can send
  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent
  case class RemovedPatient(careContactId: String, timestamp: String) extends PatientEvent*/

  // Messages I can receive
  sealed trait ElvisEvent
  case class ElvisData(data: String) extends ElvisEvent

  object attributes {
    val service = "elvisDataHandlerService"
  }
}

import sp.elvisdatahandler.{API_PatientEvent => api}

object ElvisDataHandlerComm {

  def extractElvisEvent(mess: Try[SPMessage]) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader]
      b <- m.getBodyAs[api.ElvisEvent]
    } yield (h, b)


  def makeMess(h: SPHeader, b: api.Event) = SPMessage.makeJson[SPHeader, api.Event](h, b)

}
