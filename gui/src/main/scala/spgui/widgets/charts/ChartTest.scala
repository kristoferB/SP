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
          js.Array[js.Any]("M4",2)
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

        

        val getStartTime = new js.Date(2017, 5, 9, 6, 6, 6, 6)

        def simpleTimelineDraw(rows: js.Any) =  {
          val timelineData = new GoogleVisualization.DataTable();
          timelineData.addColumn("string", "Timeline id")     //js.Dynamic.literal(`type` = "string", id = "Label id"))
          timelineData.addColumn("string", "Timeline Name")   //js.Dynamic.literal(`type` = "string", id = "Label Name"))
          timelineData.addColumn("date", "Start Date")        //js.Dynamic.literal(`type` = "date", id = "Start Date"))
          timelineData.addColumn("date", "End Date")          //js.Dynamic.literal(`type` = "date", id = "End Date"))
          
          val timelineRows = rows
          timelineData.addRows(timelineRows)

          val timelineOptions = Options("Timeline", 300,600, 14)

          val timelineElement = js.Dynamic.global.document.getElementById(id+"timeline")
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
trait OptionsTooltip extends js.Object {
  val isHtml: Boolean = js.native
  val trigger: String = js.native
}

object OptionsTooltip {
  def apply(
      isHtml: Boolean,
      trigger: String
    ) = js.Dynamic.literal(
      isHtml = isHtml,
      trigger = trigger
    )
}

@js.native
trait OptionsTimeline extends js.Object {
  val barLabelStyle:     js.Object         = js.native
  val colorByRowLabel:   Boolean           = js.native
  val groupByRowLabel:   Boolean           = js.native
  val rowLabelStyle:     js.Object         = js.native
  val showBarLabels:     Boolean           = js.native
  val showRowLabels:     Boolean           = js.native
  val singleColor:       String            = js.native
}

object OptionsTimeline {
  def apply(
    barLabelStyle:    js.Object,
    colorByRowLabel:  Boolean,
    groupByRowLabel:  Boolean,
    rowLabelStyle:    js.Object,
    showBarLabels:    Boolean,
    showRowLabels:    Boolean,
    singleColor:      String
  ) = js.Dynamic.literal(
    barLabelStyle = barLabelStyle,
    colorByRowLabel = colorByRowLabel,
    groupByRowLabel = groupByRowLabel,
    rowLabelStyle = rowLabelStyle,
    showBarLabels = showRowLabels,
    showRowLabels = showRowLabels,
    singleColor = singleColor)
}

// Documentation:     https://developers.google.com/chart/interactive/docs/gallery/timeline#configuration-options
@js.native
trait Options extends js.Object {
  val avoidOverlappingGridLines:  Boolean           = js.native
  val backgroundColor:            String            = js.native
  val colors:                     js.Array[String]  = js.native
  val enableInteractivity:        Boolean           = js.native
  val fontName:                   String            = js.native
  val fontSize:                   Int               = js.native
  val forceIFrame:                Boolean           = js.native
  val height:                     Int               = js.native
  val optionsTimeline:            OptionsTimeline   = js.native
  val optionsTooltip:             OptionsTooltip    = js.native
  val title:                      String            = js.native
  val width:                      Int               = js.native

}

object Options {
  def apply(
    title:                      String,
    height:                     Int,
    width:                      Int,   

    // how to get colors and fontSize automatically???            
    fontSize:                   Int,
    colors:                     js.Array[String] = js.Array[String]("red", "blue", "green", "white", "black"),

    avoidOverlappingGridLines:  Boolean   = true,           
    backgroundColor:            String    = "white",                 
    enableInteractivity:        Boolean   = true,           
    fontName:                   String    = "Arial",                             
    forceIFrame:                Boolean = false,                         
    optionsTimeline:            js.Object = OptionsTimeline(null, false, true, null, true, true, null),            
    optionsTooltip:             js.Object = OptionsTooltip(true, "focus")          
    ) = js.Dynamic.literal(
      avoidOverlappingGridLines = avoidOverlappingGridLines,
      backgroundColor = backgroundColor, 
      colors = colors,
      enableInteractivity = enableInteractivity,
      fontName = fontName,
      fontSize = fontSize,
      forceIFrame = forceIFrame,
      height = height,
      optionsTimeline = optionsTimeline,
      optionsTooltip = optionsTooltip,
      title = title,
      width = width
    )
}
