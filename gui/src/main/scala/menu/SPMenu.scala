package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.Grid
import spgui.circuit.{SPGUICircuit, AddWidget, SetContent}
import spgui.injection
import spgui.dashboard

object SPMenu {

  object MenuButton {
    def apply(text: String, element: ReactElement): ReactElement =
      component(MenuBtnProps(text, element))
    case class MenuBtnProps(text: String, element: ReactElement)
    val component = ReactComponentB[MenuBtnProps]("MenuButton")
      .render_P(p => <.li(p.text,
                          ^.onClick --> Callback(SPGUICircuit.dispatch(SetContent(p.element))),
                          ^.className := "btn navbar-btn"))
      .build
  }

  val widgetsConnection = SPGUICircuit.connect(_.openWidgets)

  private val component = ReactComponentB[Unit]("SPMenu")
    .render(_ =>
      <.nav(
        ^.className := "navbar navbar-static-top navbar-default",
        <.ul(
          ^.className := "nav_navbar-nav",
          MenuButton("Grid", Grid.component()),
          MenuButton("Dashboard", widgetsConnection(dashboard.Dashboard(_))),
          MenuButton("Widget Injection", injection.WidgetInjectionTest()),
          <.button(
            "Add SomeWidget",
            ^.onClick --> Callback(SPGUICircuit.dispatch(AddWidget))
          ),
          SPDropdown()
        )
      )
    )
    .build

  def apply() = component()
}
