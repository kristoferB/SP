package spgui.widgets.charts


import aleastchs.googleCharts.helpers.chartsHelp.TimelineRow
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import spgui.googleCharts.Charts
import spgui.googleCharts.DataTable
import spgui.googleCharts.Timeline

import scala.scalajs.js

object TimelineWidget {

  lazy val id = "timeline"
  lazy val idS = id + "scheme"

  case class State(zoom: String)

  private class Backend($: BackendScope[Unit, State]) {

    def render() = {
      <.div(
        ^.id := id,
        <.div(^.id := idS)
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

      Charts.load("current",
        js.Dynamic.literal(
          packages = js.Array("timeline", "corechart", "controls")
        )
      )
      Charts.setOnLoadCallback(drawFunction())

      def drawFunction(): Unit = {
        val timelineElement: js.Dynamic = js.Dynamic.global.document.getElementById(idS)

        val timeline = new Timeline(timelineElement)

        val dataTable = new DataTable()

        dataTable.addColumn("string", "TimelineString", "Row Label")
        dataTable.addColumn("string", "TimelineString", "Bar Label")
        dataTable.addColumn("date", "TimelineDate", "Start Date")
        dataTable.addColumn("date", "TimelineDate", "End Date")

        rowList.foreach(row => dataTable.addRow(row.toArray))

        val options = js.Dynamic.literal(timeline = js.Dynamic.literal(colorByRowLabel = true))

        timeline.draw(dataTable, options)

      }
      Callback.log("Mounting TimelineWidget done!!!")
    }

    def onUnmount() = {
      println("Unmounting")
      Callback.empty
    }
  }

  private val ganttComponent = ScalaComponent.builder[Unit]("ganttComponent")
    .initialState(State("100"))
    .renderBackend[Backend]
    .componentDidMount(_.backend.onMount())
    .componentWillUnmount(_.backend.onUnmount())
    .build


  def apply() = spgui.SPWidget(spwb => ganttComponent())
}