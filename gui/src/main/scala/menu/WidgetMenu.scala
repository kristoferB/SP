package spgui.menu

import japgolly.scalajs.react._

import diode.react.ModelProxy
import spgui.circuit.{SPGUICircuit, AddWidget}

object WidgetMenu {
  case class Props(proxy: ModelProxy[List[(String, ReactElement)]])

  class Backend($: BackendScope[Props, Unit]) {
    def addW(element: ReactElement): Callback =
      Callback(SPGUICircuit.dispatch(AddWidget(element)))
    def render(props: Props) =
      SPDropdown(
        "New Widget",
        for((text, element) <- props.proxy()) yield (text, addW(element))
      )
    }

  private val component = ReactComponentB[Props]("WidgetMenu")
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[List[(String, ReactElement)]]) = component(Props(proxy))
}
