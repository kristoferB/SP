package sp.elvisDataHandler

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._
import scala.util._

package API_PatientEvent {

  // Messages I can send
  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String]) extends PatientEvent
  case class RemovePatient(careContactId: String) extends PatientEvent

  // Messages I can receive
  sealed trait ElvisEvent
  case class ElvisData(data: String) extends ElvisEvent

  object attributes {
    val service = "elvisDataHandlerService"
  }
}

import sp.elvisDataHandler.{API_PatientEvent => api}

object ElvisDataHandlerComm {

  def extractElvisEvent(mess: Try[SPMessage]) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader]
      b <- m.getBodyAs[api.ElvisEvent]
    } yield (h, b)


  def makeMess(h: SPHeader, b: api.PatientEvent) = SPMessage.makeJson[SPHeader, api.PatientEvent](h, b)

}
