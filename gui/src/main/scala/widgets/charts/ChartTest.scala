package spgui.widgets.charts

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalajs.js
import scalajs.js.Dynamic
import org.scalajs.dom.raw.Element
import org.scalajs.dom.document
import java.util.concurrent.atomic.AtomicInteger
import spgui.circuit.{SPGUICircuit}

object ChartTest {
  val incrementer = new AtomicInteger(0);
  def apply() = component("ChartId-" + incrementer.incrementAndGet().toString)

  def component(id: String) = {
    ReactComponentB[Unit]("ChartTest")
      .render_P(_ => <.div(
        ^.className := ChartCSS.charts.htmlClass,
        ^.id := id,
        <.div(^.id := id+"pie"),
        <.div(^.id := id+"gantt")
      )
    )
      .componentDidMount(_ => Callback({
        println(GoogleChartsLoaded);
        if(GoogleChartsLoaded.asInstanceOf[Boolean]) {
          val data = new GoogleVisualization.DataTable();
          data.addColumn("string", "product type");
          data.addColumn("number", "produced");
          val rows = js.Array[js.Array[js.Any]](
            js.Array[js.Any]("M1",3),
            js.Array[js.Any]("M2",3),
            js.Array[js.Any]("M3",4),
            js.Array[js.Any]("M4",1)
          )
          data.addRows(rows)
          val options = js.Dynamic.literal(title = id, width=400, height=300)
          val element = js.Dynamic.global.document.getElementById(id+"pie")
          val piechart = new GoogleVisualization.PieChart(element)
          piechart.draw(data, options)

          val ganttData = new GoogleVisualization.DataTable();
          ganttData.addColumn("string", "Task ID")
          ganttData.addColumn("string", "Task Name")
          ganttData.addColumn("string", "Resource")
          ganttData.addColumn("date", "Start Date")
          ganttData.addColumn("date", "End Date")
          ganttData.addColumn("number", "Duration")
          ganttData.addColumn("number", "Percent Complete")
          ganttData.addColumn("string", "Dependencies")

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
        }
      }))
      .build.apply()
  }
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
  class DataTable extends js.Object {
    def addColumn(t: js.Any, d: js.Any): Unit = js.native
    def addRows(list: js.Any): Unit = js.native
  }
}

@js.native
trait Options extends js.Object {
  val title: js.UndefOr[String] = js.native
  val width: js.UndefOr[Int] = js.native
  val height: js.UndefOr[Int] = js.native
}
