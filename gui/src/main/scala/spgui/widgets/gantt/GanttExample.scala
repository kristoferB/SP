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
  @JSGlobal("addTheGantt")
  object addTheGantt extends js.Object {
    def apply(element: dom.Element): SPGantt = js.native
  }

  @js.native
  trait SPGantt extends js.Object {
    def addRow(): Unit = js.native
  }

  class Backend($: BackendScope[SPWidgetBase, Unit]) {
    var spGantt: SPGantt = _

    def render() =
      <.div(
        HtmlTagOf[dom.html.Element]("gantt-component"), // becomes <gantt-component></gantt-component>
        <.button("call addRow from scalajs", ^.onClick --> Callback(spGantt.addRow()))
      )
  }

  private val component = ScalaComponent.builder[SPWidgetBase]("GanttExample")
    .renderBackend[Backend]
    .componentDidMount(dcb => Callback(dcb.backend.spGantt = addTheGantt(dcb.getDOMNode)))
    .build

  def apply() = SPWidget(spwb => component(spwb))
}
