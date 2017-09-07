package spgui.widgets.gantt

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalajs.js
import js.annotation.JSGlobal
import org.scalajs.dom

import spgui.SPWidgetBase
import spgui.SPWidget

object GanttExample {

  @js.native
  trait SPGantt extends js.Object {
    def addSomeRow(): Unit = js.native
    def addRow(row: js.Object): Unit = js.native
  }
  @js.native
  @JSGlobal("SPGantt")
  object SPGantt extends js.Object {
    def apply(element: dom.Element): SPGantt = js.native
  }

  @js.native
  trait Task extends js.Object {
    var name: String = js.native
    var from: js.Date = js.native
    var to: js.Date = js.native
  }
  // atm there is no perfect way to facade "options" kind of jsObjects, but this is one way
  object Task {
    def apply(name: String, from: js.Date, to: js.Date): Task = {
      val jsObj = (new js.Object).asInstanceOf[Task]
      jsObj.name = name
      jsObj.from = from
      jsObj.to = to
      jsObj
    }
  }

  @js.native
  trait Row extends js.Object {
    var name: String = js.native
    var tasks: js.Array[Task] = js.native
  }
  object Row {
    def apply(name: String, tasks: js.Array[Task]): Row = {
      val jsObj = (new js.Object).asInstanceOf[Row]
      jsObj.name = name
      jsObj.tasks = tasks
      jsObj
    }
  }

  def somRow() = Row(
    name = "sampleRow",
    tasks = js.Array(Task("scalajs task", new js.Date(2013, 3, 30, 18, 0, 0), new js.Date(2013, 4, 12, 18, 0, 0)))
  )

  class Backend($: BackendScope[SPWidgetBase, Unit]) {
    var spGantt: SPGantt = _

    def render() =
      <.div(
        HtmlTagOf[dom.html.Element]("gantt-component"), // becomes <gantt-component></gantt-component>
        <.button("call addRow from scalajs", ^.onClick --> Callback(spGantt.addRow(somRow())))
      )
  }

  private val component = ScalaComponent.builder[SPWidgetBase]("GanttExample")
    .renderBackend[Backend]
    .componentDidMount(dcb => Callback(dcb.backend.spGantt = SPGantt(dcb.getDOMNode)))
    .build

  def apply() = SPWidget(spwb => component(spwb))
}
