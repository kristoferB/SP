package spgui.widgets.charts

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import sp.domain._
import Logic._

import scalacss.ScalaCssReact._
import spgui.communication._
import spgui.widgets.{EricaLogic, ToAndFrom, API_Patient => apiPatient, API_PatientEvent => api}
import spgui.widgets.css.{WidgetStyles => Styles}

object PatientGanttWidget {

  private class Backend($: BackendScope[String, Map[String, apiPatient.Patient]]) {

    var patientObs = Option.empty[rx.Obs]
    def setPatientObs(): Unit = {
      patientObs = Some(spgui.widgets.akuten.PatientModel.getPatientObserver(
        patients => $.setState(patients).runNow()
      ))
    }

    val wsObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
      if (mess) send(api.GetState())
    }, "patient-gantt-widget-topic")

    def send(mess: api.Event) {
      val json = ToAndFrom.make(SPHeader(from = "PatientGanttWidget", to = "WidgetService"), mess)
      BackendCommunication.publish(json, "widget-event")
    }

    def render(p: String, s: Map[String, apiPatient.Patient]) = {
      <.div(Styles.helveticaZ)
    }

    def onUnmount() = {
      println("Unmounting")
      patientObs.foreach(_.kill())
      wsObs.kill()
      Callback.empty
    }
  }

  private val ganttComponent = ScalaComponent.builder[String]("ganttComponent")
    .initialState(Map("-1" ->
      EricaLogic.dummyPatient))
    .renderBackend[Backend]
    // .componentDidMount(_.backend.getWidgetWidth())
    .componentDidMount(ctx => Callback(ctx.backend.setPatientObs()))
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def extractTeam(attributes: Map[String, SPValue]) = {
    attributes.get("team").flatMap(x => x.asOpt[String]).getOrElse("medicin")
  }

  def apply() = spgui.SPWidget(spwb => {
    val currentTeam = extractTeam(spwb.frontEndState.attributes)
    ganttComponent(currentTeam)
  })
}