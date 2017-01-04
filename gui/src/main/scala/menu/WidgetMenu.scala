package spgui.menu

import japgolly.scalajs.react.{Callback, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.circuit.{SPGUICircuit, AddWidget}

import spgui.widgets
import spgui.dashboard.Grid

object WidgetMenu {
  private def addW(element: ReactElement): Callback =
    Callback(SPGUICircuit.dispatch(AddWidget(element)))

  private val menu =
    SPDropdown(
      "New Widget",
      ("The Grid Test", addW(Grid.component())) ::
      ("Widget Injection", addW(widgets.injection.WidgetInjectionTest())) ::
      ("CommTest", addW(widgets.WidgetCommTest())) ::
      ("SomePlaceHolder", addW(<.h2("SomePlaceHolder"))) ::
      Nil
    )

  def apply() = menu
}
