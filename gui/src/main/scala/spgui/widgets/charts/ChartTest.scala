package spgui.widgets.charts

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalajs.js
import scalajs.js.Dynamic
import org.scalajs.dom.raw.Element
import org.scalajs.dom.document
import java.util.concurrent.atomic.AtomicInteger
import spgui.circuit.{SPGUICircuit}
import spgui.communication._

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
        ^.className := ChartCSS.charts.htmlClass,
        ^.id := id,
        <.div(^.id := id+"pie"),
        <.div(^.id := id+"gantt"),
        <.div(^.id := id+"timeline")
      )

    def onMount() = {
      println(GoogleChartsLoaded);
      if(GoogleChartsLoaded.asInstanceOf[Boolean]) {
        val data = new GoogleVisualization.DataTable();
        data.addColumn("string", "productType")
        //js.Dynamic.literal(`type` = "string", id = "product type"));
        data.addColumn("number", "produced");
        val rows = js.Array[js.Array[js.Any]](
          js.Array[js.Any]("M1",3),
          js.Array[js.Any]("M2",3),
          js.Array[js.Any]("M3",4),
          js.Array[js.Any]("M4",1)
        )
        data.addRows(rows)
        val options = js.Dynamic.literal(title = id, width=400, height=300, is3D = true)
        val element = js.Dynamic.global.document.getElementById(id+"pie")
        val piechart = new GoogleVisualization.PieChart(element)
        piechart.draw(data, options)

        val ganttData = new GoogleVisualization.DataTable();
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


        val timelineData = new GoogleVisualization.DataTable();
        timelineData.addColumn("string", "Timeline id")     //js.Dynamic.literal(`type` = "string", id = "Label id"))
        timelineData.addColumn("string", "Timeline Name")   //js.Dynamic.literal(`type` = "string", id = "Label Name"))
        timelineData.addColumn("date", "Start Date")        //js.Dynamic.literal(`type` = "date", id = "Start Date"))
        timelineData.addColumn("date", "End Date")          //js.Dynamic.literal(`type` = "date", id = "End Date"))

        val timelineRows = js.Array[js.Array[js.Any]](
          js.Array[js.Any]("spring", "startup", new js.Date(2014, 2, 22, 20, 3, 0, 0), new js.Date(2014, 2, 22, 20, 3, 55, 0)),
          js.Array[js.Any]("summer", "beach", new js.Date(2014, 2, 22, 20, 3, 0, 0), new js.Date(2014, 2, 22, 20, 3, 45, 0)),
          js.Array[js.Any]("autumn", "rain", new js.Date(2014, 2, 22, 20, 3, 0, 0), new js.Date(2014, 2, 22, 20, 3, 48, 0)),
          js.Array[js.Any]("winter", "snow", new js.Date(2014, 2, 22, 20, 3, 0, 0), new js.Date(2014, 2, 22, 20, 3, 52, 0)),
          js.Array[js.Any]("spring", "bloom", new js.Date(2014, 2, 22, 20, 3, 0, 0), new js.Date(2014, 2, 22, 20, 3, 30, 20)),
          js.Array[js.Any]("summer", "sun", new js.Date(2014, 2, 22, 20, 3, 0, 0), new js.Date(2014, 2, 22, 20, 3, 20, 0))
        )



        timelineData.addRows(timelineRows)

        val timelineOptions = js.Dynamic.literal(title = id, height=300, width=600,
          timeline = js.Dynamic.literal(showRowLabels = true, colorByRowLabel = true),
          hAxis = js.Dynamic.literal(
            viewWindow = js.Dynamic.literal(
              min = new js.Date(2014, 2, 22, 20, 2, 59, 30),
              max = new js.Date(2014, 2, 22, 20, 4, 0, 0)
            )
          )
        )

        val timelineElement = js.Dynamic.global.document.getElementById(id+"timeline")
        val timeline = new GoogleVisualization.Timeline(timelineElement)
        timeline.draw(timelineData, timelineOptions)
      }
      Callback.empty
    }

    def onUnmount() = {
      println("Unmounting charts")
      eventHandler.kill()
      Callback.empty
    }

  }

  private val component = ReactComponentB[Unit]("ChartTest")
    .initialState(State(x = 5))
    .renderBackend[Backend]
    .componentDidMount(_.backend.onMount())
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())

}

@js.native
object GoogleChartsLoaded extends js.Object

@js.native
object GoogleVisualization extends js.Object {
  @js.native
  class PieChart(element: js.Dynamic) extends js.Object {
    def draw(data: js.Any, options: js.Object): Unit = js.native
  }
  @js.native
  class Gantt(element: js.Dynamic) extends js.Object {
    def draw(data: js.Any, options: js.Object): Unit = js.native
  }
  @js.native
  class Timeline(element: js.Dynamic) extends js.Object {
    def draw(data: js.Any, options: js.Object): Unit = js.native
  }
  @js.native
  class DataTable extends js.Object {
    def addColumn(t: js.Any, d: js.Any): Unit = js.native
    //def addColumn(obj: js.Object): Unit = js.native
    def addRows(list: js.Any): Unit = js.native
  }
}

@js.native
trait Options extends js.Object {
  val title: js.UndefOr[String] = js.native
  val width: js.UndefOr[Int] = js.native
  val height: js.UndefOr[Int] = js.native
}
