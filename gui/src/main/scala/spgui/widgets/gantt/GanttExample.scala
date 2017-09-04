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
  @JSGlobal("angular")
  object angular extends js.Object {
    def bootstrap(element: dom.Element, apps: js.Array[String]): Unit = js.native
  }

  private val component = ScalaComponent.builder[SPWidgetBase]("GanttExample")
    .render_P(p => HtmlTagOf[dom.html.Element]("gantt-component")("hejhej")) // becomes <gantt-component>hejhej</gantt-component>
    .componentDidMount(dcb => Callback(angular.bootstrap(dcb.getDOMNode, js.Array("ganttApp"))))
    .build

  def apply() = SPWidget(spwb => component(spwb))
}
