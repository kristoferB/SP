package spgui.widgets.charts

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import sp.domain.SPValue

import scalacss.ScalaCssReact._
import sp.messages.Pickles.SPHeader
import spgui.communication._
import spgui.widgets.{API_Patient => apiPatient, API_PatientEvent => api}
import spgui.widgets.ToAndFrom
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

    def send(mess: api.StateEvent) {
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
      apiPatient.Patient(
        "4502085",
        apiPatient.Priority("NotTriaged", "2017-02-01T15:49:19Z"),
        apiPatient.Attended(true, "SARLI29", "2017-02-01T15:58:33Z"),
        apiPatient.Location("52", "2017-02-01T15:58:33Z"),
        apiPatient.Team("GUL", "NAKME", "B", "2017-02-01T15:58:33Z"),
        apiPatient.Examination(false, "2017-02-01T15:58:33Z"),
        apiPatient.LatestEvent("OmsKoord", -1, false, "2017-02-01T15:58:33Z"),
        apiPatient.Plan(false, "2017-02-01T15:58:33Z"),
        apiPatient.ArrivalTime("", "2017-02-01T10:01:38Z"),
        apiPatient.Debugging("NAKKK","B","B23"),
        apiPatient.Finished(false, false, "2017-02-01T10:01:38Z")
      )))
    .renderBackend[Backend]
    // .componentDidMount(_.backend.getWidgetWidth())
    .componentDidMount(ctx => Callback(ctx.backend.setPatientObs()))
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def extractTeam(attributes: Map[String, SPValue]) = {
    attributes.get("team").map(x => x.str).getOrElse("medicin")
  }

  def apply() = spgui.SPWidget(spwb => {
    val currentTeam = extractTeam(spwb.frontEndState.attributes)
    ganttComponent(currentTeam)
  })
}