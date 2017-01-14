package spgui.menu

import japgolly.scalajs.react._

import diode.react.ModelProxy
import spgui.circuit.{SPGUICircuit, AddWidget}
import spgui.WidgetList

object WidgetMenu {
  case class Props(proxy: ModelProxy[List[String]])

  class Backend($: BackendScope[Props, Unit]) {
    def addW(widgetType: String): Callback =
      Callback(SPGUICircuit.dispatch(AddWidget(widgetType)))
    def render(props: Props) =
      SPDropdown(
        "New Widget",
        for(widgetType <- props.proxy()) yield (widgetType, addW(widgetType))
      )
    }

  private val component = ReactComponentB[Props]("WidgetMenu")
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[List[String]]) = component(Props(proxy))
}
