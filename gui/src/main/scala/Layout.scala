package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.circuit.SPGUICircuit
import spgui.menu.SPMenu
import spgui.dashboard.Dashboard

object Layout {
  val widgetsConnection = SPGUICircuit.connect(_.openWidgets)

  private val component = ReactComponentB[Unit]("Layout")
    .render(_ =>
      <.div(
        SPMenu(),
        widgetsConnection(Dashboard(_))
      )
    )
    .build

  def apply() = component()
}
