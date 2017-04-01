package sp.patientcardsservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._



package API_PatientEvent {

  // messages I can recieve
  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent
  case class RemovedPatient(careContactId: String) extends PatientEvent
  // case class NewPatient( careContactId: String, patientData: Map[String,Any]) extends PatientEvent
  // case class DiffPatient( careContactId: String, patientData: Map[String,Any]) extends PatientEvent
  // case class RemovedPatient( careContactId: String) extends PatientEvent

  // sealed trait API_PatientCardsDevice
  // case class NewPatient() extends API_PatientCardsDevice
  // case class DiffPatient() extends API_PatientCardsDevice
  // case class RemovedPatient() extends API_PatientCardsDevice
  // case class Patient( careContactId: String, patientData: Map[String,Any]) extends API_PatientCardsDevice
  // case class elvisEvent( eventType: String, patient: Patient) extends API_PatientCardsDevice
  // case class State(state: State) extends API_PatientCardsDevice

  object attributes {
    val service = "patientCardsService"
}
}

import sp.patientcardsservice.{API_PatientEvent => api}

object PatientCardsComm {
  def extractRequest(mess: Try[SPMessage]) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader] // if h.to == instanceID.toString || h.to == name || h.to == api.attributes.service
      b <- m.getBodyAs[api.Request]
    } yield (h, b)

  // def extractServiceRequest(mess: Try[SPMessage]) = for {
  //   m <- mess
  //   h <- m.getHeaderAs[SPHeader]
  //   b <- m.getBodyAs[APISP.StatusRequest]
  //   } yield (h, b)


  def makeMess(h: SPHeader, b: api.PatientEvent) = SPMessage.makeJson[SPHeader, api.PatientEvent](h, b)
  //def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)

}
