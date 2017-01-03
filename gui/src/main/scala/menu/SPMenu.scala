package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.circuit.{SPGUICircuit, AddWidget}

object SPMenu {
  private val component = ReactComponentB[Unit]("SPMenu")
    .render(_ =>
      <.nav(
        ^.className := "navbar navbar-static-top navbar-default",
        <.ul(
          ^.className := "nav_navbar-nav",
          WidgetMenu()
        )
      )
    )
    .build

  def apply() = component()
}
