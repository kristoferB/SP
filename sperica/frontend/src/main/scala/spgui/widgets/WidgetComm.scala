package spgui.widgets

import sp.domain._
import Logic._
import spgui.communication.BackendCommunication

import scala.util.Try

import sp.erica._



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




object ToAndFrom {
  def eventBody(mess: SPMessage): Try[API_PatientEvent.Event] = mess.getBodyAs[API_PatientEvent.Event]

  def make(h: SPHeader, b: API_PatientEvent.Event): SPMessage = SPMessage.make(h, b)
}
