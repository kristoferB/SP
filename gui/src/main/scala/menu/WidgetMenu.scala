package spgui.menu

import japgolly.scalajs.react._

import spgui.circuit.{SPGUICircuit, AddWidget}
import spgui.WidgetList
import spgui.Icon
import japgolly.scalajs.react.vdom.prefix_<^._

object WidgetMenu {
  class Backend($: BackendScope[Unit, Unit]) {
    def addW(widgetType: String): Callback =
      Callback(SPGUICircuit.dispatch(AddWidget(widgetType)))
    def render =
      SPDropdown(
        for(widgetType <- WidgetList().keys.toList) yield (widgetType, addW(widgetType)),
        Icon.windowMaximize,
        "New widget"
      )
  }

  private val component = ReactComponentB[Unit]("WidgetMenu")
    .renderBackend[Backend]
    .build

  def apply() = component()
}
