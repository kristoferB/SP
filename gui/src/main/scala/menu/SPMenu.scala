package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.circuit.SPGUICircuit

object SPMenu {
  val availableWidgetsConnection = SPGUICircuit.connect(_.availableWidgets)

  private val component = ReactComponentB[Unit]("SPMenu")
    .render(_ =>
    <.nav(
      ^.className := SPMenuCSS.topNav.htmlClass,
      ^.className := "navbar navbar-static-top navbar-default",
      <.ul(
        ^.className := SPMenuCSS.buttonList.htmlClass,
        ^.className := "nav_navbar-nav",
        availableWidgetsConnection(WidgetMenu(_))
      )
    )
  )
    .build

  def apply() = component()
}
