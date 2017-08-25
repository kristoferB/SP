package spgui.widgets.charts


import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.svg

import scalacss.ScalaCssReact._
import sp.domain.SPValue
import sp.messages.Pickles.SPHeader
import spgui.communication._
import spgui.widgets.{API_Patient => apiPatient, API_PatientEvent => api}
import spgui.widgets.ToAndFrom
import spgui.widgets.css.{WidgetStyles => Styles}
import aleastchs.googleCharts.GoogleVisualization
import aleastchs.googleCharts.GoogleChartsLoaded
import aleastchs.googleCharts.helpers.chartsHelp.TimelineRow

import scala.scalajs.js
import scala.scalajs.js.Any
import scala.scalajs.js.annotation.JSGlobal

object PatientGanttWidget {

  val id = "patientGanttWidget"

  private class Backend($: BackendScope[String, Map[String, apiPatient.Patient]]) {

    var patientObs = Option.empty[rx.Obs]
    def setPatientObs(): Unit = {
      patientObs = Some(spgui.widgets.akuten.PatientModel.getPatientObserver(
        patients => $.setState(patients).runNow()
      ))
    }

    val wsObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
      if (mess) send(api.GetState())
    }, "patient-cards-widget-topic")

    def send(mess: api.StateEvent) {
      val json = ToAndFrom.make(SPHeader(from = "PatientGanttWidget", to = "WidgetService"), mess)
      BackendCommunication.publish(json, "widget-event")
    }

    def render(p: String, s: Map[String, apiPatient.Patient]) = {
      <.div(Styles.helveticaZ)
      <.div(
        ^.className := GanttCSS.background.htmlClass,
        ^.id := id,
        <.div(^.id := id + "scheme")
      )
    }

    /*********EXAMPLE USE OF GOOGLE API WITH Helper-class*************/
    val rowList: List[TimelineRow] =
      TimelineRow("Besök", "Patientens Besök På Sjukhuset",
        new js.Date(2017, 5, 20, 8, 5, 3, 2), new js.Date(2017, 5, 20, 10, 32, 23, 9)) ::
        TimelineRow("Kölapp", "Tar Kölapp",
          new js.Date(2017, 5, 20, 8, 6, 13, 8), new js.Date(2017, 5, 20, 8, 6, 31, 7)) ::
        TimelineRow("Väntetid", "Patient Väntar På inskrivning",
          new js.Date(2017, 5, 20, 8, 6, 31, 7), new js.Date(2017, 5, 20, 8, 23, 54, 1)) ::
        TimelineRow("Inskrivning", "Patient Skriver in sig",
          new js.Date(2017, 5, 20, 8, 24, 11, 2), new js.Date(2017, 5, 20, 8, 26, 46, 3)) ::
        TimelineRow("Väntetid", "Patienten väntar på läkare",
          new js.Date(2017, 5, 20, 8, 26, 46, 3), new js.Date(2017, 5, 20, 9, 1, 35, 4)) ::
        TimelineRow("Läkarbesök", "Patient träffar läkare",
          new js.Date(2017, 5, 20, 9, 1, 35, 4), new js.Date(2017, 5, 20, 9, 9, 21, 5)) ::
        TimelineRow("Väntetid", "Patient väntar på diagnos",
          new js.Date(2017, 5, 20, 9, 9, 21, 5), new js.Date(2017, 5, 20, 9, 59, 1, 0)) ::
        TimelineRow("Diagnostiering", "Läkare sätter diagnos",
          new js.Date(2017, 5, 20, 9, 9, 21, 5), new js.Date(2017, 5, 20, 9, 27, 54, 9)) ::
        TimelineRow("Läkarbesök", "Patient träffar läkare",
          new js.Date(2017, 5, 20, 9, 59, 1, 0), new js.Date(2017, 5, 20, 10, 14, 13, 4)) ::
        Nil

    def onMount() = {
      println("Hej Google Charts Mount")

      println(GoogleChartsLoaded)
      if(GoogleChartsLoaded.asInstanceOf[Boolean])
        drawFunction()
      else
        println("Could not draw and load the google charts")


      def drawFunction(): Unit = {
        val timelineElement: js.Dynamic = js.Dynamic.global.document.getElementById(id+"scheme")

        val timeline = new GoogleVisualization.Timeline(timelineElement)

        val dataTable = new GoogleVisualization.DataTable()

        dataTable.addColumn("string", "TimelineString", "Row Label")
        dataTable.addColumn("string", "TimelineString", "Bar Label")
        dataTable.addColumn("date", "TimelineDate", "Start Date")
        dataTable.addColumn("date", "TimelineDate", "End Date")

        rowList.foreach(row => dataTable.addRow(row.toArray))

        val options = js.Dynamic.literal(timeline = js.Dynamic.literal(colorByRowLabel = true))

        timeline.draw(dataTable, options)

      }

      //        val timelineHelper = TimelineHelper(timelineElement, "Gantt För Patienter")
      //
      //        rowList.foreach(row => timelineHelper.newRow(row))
      //
      //        timelineHelper.draw()
      //        //onUpdate(rowList, timelineHelper)

      Callback.log("Mounting Done! Widget Gantt")
    }

    /*
    /** When EricaPatients from Google Pub/Sub is updated,
      * clear gantt scheme.
      *
      * Then add the rows to the helper with new updated data.
      *
      * Draw the new rows
      *
      * @note Precondition: The timelineHelper must have a div-element where to be printed
      *
      * @param rows                   A List of TimelineRow
      * @param helperToUse      A TimelineHelper from scalajs-google-charts library
      *
      * @return the new TimelineHelper that just drawn the updated gantt scheme
      */
      * */
    /*def onUpdate(rows: List[TimelineRow], helperToUse: TimelineHelper): TimelineHelper = {
      helperToUse.clear()

      rows.foreach(row => helperToUse.newRow(row))

      helperToUse.draw()

      helperToUse
    }*/

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
    .componentDidMount(_.backend.onMount())
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def extractTeam(attributes: Map[String, SPValue]) = {
    attributes.get("team").map(x => x.str).getOrElse("medicin")
  }

  //def apply() = spgui.SPWidget(spwb => ganttComponent())

  def apply() = spgui.SPWidget(spwb => {
    val currentTeam = extractTeam(spwb.frontEndState.attributes)
    ganttComponent(currentTeam)
  })
}