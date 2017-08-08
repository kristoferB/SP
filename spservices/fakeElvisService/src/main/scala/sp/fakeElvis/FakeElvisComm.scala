package sp.fakeElvis

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._
import scala.util._

package API_PatientEvent {
  
  // Messages I can send
  sealed trait ElvisEvent
  case class ElvisData(data: String) extends ElvisEvent

  object attributes {
    val service = "fakeElvis"
  }
}

import sp.fakeElvis.{API_PatientEvent => api}

object FakeElvisComm {

  def makeMess(h: SPHeader, b: api.ElvisEvent) = SPMessage.makeJson[SPHeader, api.ElvisEvent](h, b)

}
