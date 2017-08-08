package spgui.widgets.akuten

import spgui.communication.BackendCommunication
import spgui.widgets.{API_Patient, API_PatientEvent, ToAndFrom}

/**
  * Created by kristofer on 2017-05-02.
  */
object PatientModel {
  import rx._
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val model = new PatienModel


  def getPatientObserver(callBack: (Map[String, API_Patient.Patient]) => Unit): rx.Obs = {
    model.pats.foreach(callBack)
  }


}

class PatienModel {
  import rx._
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val upd = Var(Map[String, API_Patient.Patient]())
  val pats = Var(Map[String, API_Patient.Patient]())
  val prev = Var(Map[String, API_Patient.Patient]())

  val messObs = BackendCommunication.getMessageObserver(
    mess => {
      ToAndFrom.eventBody(mess).map {
        case API_PatientEvent.State(patients) =>
          upd() = patients
        case _ => println("something else in PatientModel: " + mess)
      }
    }
    , "patient-cards-widget-topic")

  val checkPrev = Rx {
    val u = upd()
    val p = prev()
    if (u != p) {  // verkar inte fungera då det alltid är skillnad...
      pats() = u
      prev() = u
    }
  }
}