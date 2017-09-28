package spgui.widgets.gantt

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalajs.js
import js.JSConverters._
import js.timers._
import org.scalajs.dom
import spgui.SPWidgetBase
import spgui.SPWidget

import scala.collection.mutable

sealed trait API_LiveGantt
object API_LiveGantt {
  case class Tick(epochmillis: Int) extends API_LiveGantt
  case class ActivityStart(epochmillis: Int, category: String, description: String) extends API_LiveGantt
  case class ActivityStop(epochmillis: Int, category: String, description: String) extends API_LiveGantt
}

object SampleEventPlayback {
  import API_LiveGantt._
  def apply(): List[API_LiveGantt] = {
    val ticks = (0 to 30).map(s => API_LiveGantt.Tick(s * 1000)).toList
    val startsAndStops = List(
      ActivityStart(1 * 1000 + 18, "EventKindA", ""),
      ActivityStop(9 * 1000 + 8, "EventKindA", ""),
      ActivityStart(7 * 1000 + 618, "EventKindB", ""),
      ActivityStop(18 * 1000 + 701, "EventKindB", ""),
      ActivityStart(15 * 1000 + 0, "EventKindA", ""),
      ActivityStop(22 * 1000 + 399, "EventKindA", "")
    )
    (ticks ::: startsAndStops).sortBy {
      case x: Tick => x.epochmillis
      case x: ActivityStart => x.epochmillis
      case x: ActivityStop => x.epochmillis
    }
  }
}

object State {
  import API_LiveGantt._

  case class Activity(start: Int, end: Option[Int])
  val activities = mutable.Map[String, mutable.ListBuffer[Activity]]()
  var secondsElapsed = 0

  def handleEvent: API_LiveGantt => js.Array[Row] = {
    //case Tick(epochmillis) =>
      //activities.keys.foreach(k => activities(k) = activities(k).map(a => a.copy(end = Option(a.end.getOrElse(epochmillis)))))
    case Tick(epochmillis) => {
      secondsElapsed = epochmillis / 1000
      ganttFormat(epochmillis)
    }
    case ActivityStart(epochmillis, category, _) => {
      if (activities.contains(category)) activities(category) += Activity(epochmillis, None)
      else activities(category) = mutable.ListBuffer(Activity(epochmillis, None))
      ganttFormat(epochmillis)
    }
    case ActivityStop(epochmillis, category, _) => {
      activities(category) = activities(category).map(a => if (a.end.isDefined) a else a.copy(end = Some(epochmillis)))
      ganttFormat(epochmillis)
    }
  }

  def ganttFormat(epochmillis: Int) =
    activities.map {kv =>
      Row(
        kv._1,
        kv._2.map(a => Task("", new js.Date(a.start), new js.Date(a.end.getOrElse(epochmillis)))).toJSArray
      )
    }.toJSArray

}

object LiveGanttExample {

  class Backend($: BackendScope[SPWidgetBase, List[API_LiveGantt]]) {
    var spGantt: SPGantt = _
    var interval: js.UndefOr[js.timers.SetIntervalHandle] =
      js.undefined

    def headOp(s: List[API_LiveGantt]) = CallbackTo(s.headOption)
    // TODO not sure if we should throw a whole new copy of the data to angular-gantt upon each event as done here
    // some of its features may require we let it keep track of its state itself
    def handleEvent(headOp: Option[API_LiveGantt]) = Callback {
      if (headOp.isDefined) spGantt.setData(State.handleEvent(headOp.get))
      else interval.foreach(clearInterval)
    }
    def dropHead: Callback = $.modState(s => if (!s.isEmpty) s.tail else s)
    def simulateNextEvent = $.state >>= headOp >>= handleEvent >>= (_ => dropHead)
    def startSimulation = Callback {
      interval = setInterval(250) {
        simulateNextEvent.runNow()
      }
    }

    def clear = $.setState(List()) >> Callback(spGantt.setData(js.Array()))

    def render(s: List[API_LiveGantt]) =
      <.div(
        HtmlTagOf[dom.html.Element]("gantt-component"), // becomes <gantt-component></gantt-component>
        <.h3("seconds elapsed: " + State.secondsElapsed),
        <.button("start simulation", ^.onClick --> startSimulation)
      )
  }

  private val component = ScalaComponent.builder[SPWidgetBase]("LiveGanttExample")
    .initialState(SampleEventPlayback())
    .renderBackend[Backend]
    .componentDidMount(dcb => Callback{
      dcb.backend.spGantt = SPGantt(dcb.getDOMNode, SPGanttOptions(headers = js.Array("second"), viewScale = "2 seconds"))
      //dcb.backend.spGantt.setData(ganttData)
    })
    .build

  def apply() = SPWidget(spwb => component(spwb))
}
