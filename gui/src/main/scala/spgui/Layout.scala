package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalajs.js
import js.annotation.JSGlobal
import org.scalajs.dom

import spgui.circuit.SPGUICircuit
import spgui.menu.SPMenu
import spgui.dashboard.Dashboard

object Layout {

  @js.native
  @JSGlobal("bootstrapGantt")
  object bootstrapGantt extends js.Any {
    def apply(element: dom.raw.HTMLElement): Unit = js.native
  }

  val widgetsConnection = SPGUICircuit.connect(x => (x.openWidgets.xs, x.globalState))
  val menuConnection = SPGUICircuit.connect(x => (x.settings))

  val component = ScalaComponent.builder[Unit]("Layout")
    .render(_ =>
      <.div(
        ^.className := GlobalCSS.layout.htmlClass,
        menuConnection(SPMenu(_)),
        widgetsConnection(Dashboard(_))
      )
    )
    .componentDidMount(dcb => Callback(bootstrapGantt(dom.document.body)))
    .build

  def apply() = component()
}
