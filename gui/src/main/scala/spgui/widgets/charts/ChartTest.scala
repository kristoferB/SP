package spgui.widgets.charts

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalajs.js
import scalajs.js.Dynamic
import org.scalajs.dom.raw.Element
import org.scalajs.dom.document
import java.util.concurrent.atomic.AtomicInteger

import spgui.circuit.SPGUICircuit
import spgui.communication._
import spgui.googleCharts.GoogleChartsLoaded
import aleastchs.googleCharts.google.visualization.Timeline
import aleastchs.googleCharts.google.visualization.DataTable
import aleastchs.googleCharts.helpers.chartsHelp._

import scala.scalajs.js.annotation.JSName

object ChartTest {
  case class State(x: Int)

  val id = "test"

  private class Backend($: BackendScope[Unit, State]) {
    val eventHandler = BackendCommunication.getMessageObserver(
      mess => {
        println("[Charts] Got event " + mess.toString)
      },
      "events"
    )

    def render(s: State) =
      <.div(
        <.div(
          ^.className := ChartCSS.charts.htmlClass,
          ^.id := id,
          <.p("Hello To Chart!!!")
        ),
        <.div(
          ^.className := ChartCSS.charts.htmlClass,
          ^.id := id,
          <.div(^.id := id+"pie"),
          <.div(^.id := id+"gantt"),
          <.div(
            ^.id := id+"timeline")
        )
      )

    def onMount() = {
      println(GoogleChartsLoaded)
      if(GoogleChartsLoaded.asInstanceOf[Boolean]) {
        val timelineElement = js.Dynamic.global.document.getElementById(id+"timeline")
        val ganttElement = js.Dynamic.global.document.getElementById(id+"gantt")
        val timeline = new Timeline(timelineElement)

        val data = new DataTable()
        data.addColumn("string", "Timeline id", "1")
        data.addColumn("string", "Timeline Name", "2")
        data.addColumn("date", "Start Date", "4")
        data.addColumn("date", "End Date", "5")

        val exampleRow = TimelineRow("row", "bar", null, new js.Date(2014, 2, 22), new js.Date(2014, 5, 20), 4)

        data.addRow(exampleRow.toArray)

        val exampleOptions = TimelineOption(300,400, "example")

        timeline.draw(data, exampleOptions.toDynamic)
      }
      Callback.log("Mounting ChartTest Done!")
    }

    def onUnmount() = {
      println("Unmounting charts")
      eventHandler.kill()
      Callback.empty
    }

  }

  private val component = ScalaComponent.builder[Unit]("ChartTest")
    .initialState(State(x = 5))
    .renderBackend[Backend]
    .componentDidMount(_.backend.onMount())
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())

}

/*
                val getStartTime = new js.Date(2017, 5, 9, 6, 6, 6, 6)

                def simpleTimelineDraw(rows: js.Any) =  {
                  val timelineData = new GoogleVisualization.DataTable()
                  timelineData.addColumn("string", "Timeline id")     //js.Dynamic.literal(`type` = "string", id = "Label id"))
                  timelineData.addColumn("string", "Timeline Name")   //js.Dynamic.literal(`type` = "string", id = "Label Name"))
                  timelineData.addColumn("date", "Start Date")        //js.Dynamic.literal(`type` = "date", id = "Start Date"))
                  timelineData.addColumn("date", "End Date")          //js.Dynamic.literal(`type` = "date", id = "End Date"))

                  val timelineRows = rows
                  timelineData.addRows(timelineRows)

                  val timelineOptions = OptionsTimeline(300,600)

                  val timelineElement = js.Dynamic.global.document.getElementById("timelineTesting")
                  val timeline = new GoogleVisualization.Timeline(timelineElement)
                  timeline.draw(timelineData, timelineOptions)
                }

                def simpleUpdateRows(start: js.Date, end: js.Date): js.Any = {
                  js.Array[js.Array[js.Any]](
                    js.Array[js.Any]("spring", "startup",   start,   end),
                    js.Array[js.Any]("summer", "beach",     start,   end),
                    js.Array[js.Any]("autumn", "rain",      start,   end),
                    js.Array[js.Any]("winter", "snow",      start,   end),
                    js.Array[js.Any]("spring", "bloom",     start,   end),
                    js.Array[js.Any]("summer", "sun",       start,   end)
                  )
                }

                simpleTimelineDraw(simpleUpdateRows(getStartTime, new js.Date()))
                */

/*
        val data = new GoogleVisualization.DataTable()
        data.addColumn("string", "productType")
        //js.Dynamic.literal(`type` = "string", id = "product type"));
        data.addColumn("number", "produced")
        val rows = js.Array[js.Array[js.Any]](
          js.Array[js.Any]("M1",3),
          js.Array[js.Any]("M2",3),
          js.Array[js.Any]("M3",4),
          js.Array[js.Any]("M4",2)
        )
        data.addRows(rows)
        val options = js.Dynamic.literal(title = id, width=400, height=300, is3D = true)
        val element = js.Dynamic.global.document.getElementById(id+"pie")
        val piechart = new GoogleVisualization.PieChart(element)
        piechart.draw(data, options)

        val ganttData = new GoogleVisualization.DataTable()
        ganttData.addColumn("string", "Task Id")
        ganttData.addColumn("string", "Task Name")    //js.Dynamic.literal(`type` = "string", id = "Task Name"))
        ganttData.addColumn("string", "Resource")     // js.Dynamic.literal(`type` = "string", id = "Resource"))
        ganttData.addColumn("date", "Start Date")     //js.Dynamic.literal(`type` = "date", id = "Start Date"))
        ganttData.addColumn("date", "End Date")       //js.Dynamic.literal(`type` = "date", id = "End Date"))
        ganttData.addColumn("number", "Duration")     //js.Dynamic.literal(`type` = "number", id = "Duration"))
        ganttData.addColumn("number", "Percent Complete") //js.Dynamic.literal(`type` = "number", id = "Percent Complete"))
        ganttData.addColumn("string", "Dependencies")     //js.Dynamic.literal(`type` = "string", id = "Dependencies"))


        val ganttRows = js.Array[js.Array[js.Any]](
          js.Array[js.Any]("2014Spring","Spring 2014", "spring", new js.Date(2014, 2, 22), new js.Date(2014, 5, 20), null, 100, null),
          js.Array[js.Any]("2014Spring","Spring 2014", "spring", new js.Date(2014, 2, 22), new js.Date(2014, 5, 20), null, 100, null),
          js.Array[js.Any]("2014Summer","Summer 2014", "summer", new js.Date(2014, 5, 21), new js.Date(2014, 8, 20), null, 100, null),
          js.Array[js.Any]("2014Autumn","Autumn 2014", "autumn", new js.Date(2014, 8, 21), new js.Date(2014, 11, 20), null, 100, null),
          js.Array[js.Any]("2014Winter","Winter 2014", "winter", new js.Date(2014, 11, 21), new js.Date(2015, 2, 21), null, 100, null),
          js.Array[js.Any]("2015Spring","Spring 2015", "spring", new js.Date(2015, 2, 22), new js.Date(2015, 5, 20), null, 50, null),
          js.Array[js.Any]("2015Summer","Summer 2015", "summer", new js.Date(2015, 5, 21), new js.Date(2015, 8, 20), null, 0, null),
          js.Array[js.Any]("Hockey","Hockey Season", "sports", new js.Date(2014, 9, 8), new js.Date(2015, 5, 21), null, 89, null)
        )
        ganttData.addRows(ganttRows)

        val ganttOptions = js.Dynamic.literal(title = id, height=300, width=600, gantt = js.Dynamic.literal(trackHeight = 30))
        val ganttElement = js.Dynamic.global.document.getElementById(id+"gantt")
        val gantt = new GoogleVisualization.Gantt(ganttElement)
        gantt.draw(ganttData, ganttOptions)


*/
