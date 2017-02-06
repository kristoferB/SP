package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.circuit.SPGUICircuit
import spgui.menu.SPMenu
import spgui.dashboard.Dashboard

object Layout {
  val widgetsConnection = SPGUICircuit.connect(_.openWidgets.list)

  private val component = ReactComponentB[Unit]("Layout")
    .render(_ =>
    <.div(
      ^.className := GlobalCSS.layout.htmlClass,
      SPMenu(),
      SPButton("testar en knapp"),
      widgetsConnection(Dashboard(_))
    )
  )
    .build

  def apply() = component()
}
