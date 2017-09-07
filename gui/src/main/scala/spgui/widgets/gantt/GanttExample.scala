package spgui.widgets.gantt

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalajs.js
import org.scalajs.dom

import spgui.SPWidgetBase
import spgui.SPWidget

object GanttExample {

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
