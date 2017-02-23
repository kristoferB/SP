package spgui.menu

import japgolly.scalajs.react._

import spgui.circuit.{SPGUICircuit, AddWidget}
import spgui.WidgetList
import spgui.Icon
import japgolly.scalajs.react.vdom.prefix_<^._

object WidgetMenu {
  class Backend($: BackendScope[Unit, Unit]) {
    def addW(name: String, w: Int, h: Int): Callback =
      Callback(SPGUICircuit.dispatch(AddWidget(name, w, h)))
    def render =
      SPDropdown(
        for(widget <- WidgetList.list) yield (widget._1, addW(widget._1, widget._3, widget._4)),
        Icon.windowMaximize,
        "New widget"
      )
  }

  private val component = ReactComponentB[Unit]("WidgetMenu")
    .renderBackend[Backend]
    .build

  def apply() = component()
}
