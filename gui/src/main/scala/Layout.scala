package spgui

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom.document
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object Layout {

  val widgetsConnection = AppCircuit.connect(_.openWidgets)
  val contentConnection = AppCircuit.connect(_.content)

  case class State(component: ReactElement)

  class Backend($: BackendScope[Unit, State]) {
    def changeState(s: ReactElement): Callback = $.setState(State(s))
    def render(p: Unit, s: State) =
      <.div(
        <.nav(
          ^.className := "navbar navbar-static-top navbar-default",
          <.ul(
            ^.className := "nav_navbar-nav",
            MenuButton("Grid", Grid.component()),
            MenuButton("Dashboard", widgetsConnection(dashboard.Dashboard(_))),
            MenuButton("Widget Injection", injection.WidgetInjectionTest()),
            <.button(
              "Add SomeWidget",
              ^.onClick --> Callback(AppCircuit.dispatch(AddWidget))
            ),
            SPDropdown()
          )
        ),
        contentConnection(SPContentPane(_))
      )

    object MenuButton {
      def apply(text: String, element: ReactElement): ReactElement =
        component(MenuBtnProps(text, element))
      case class MenuBtnProps(text: String, element: ReactElement)
      val component = ReactComponentB[MenuBtnProps]("MenuButton")
        .render_P(p => <.li(p.text,
                            ^.onClick --> Callback(AppCircuit.dispatch(SetContent(p.element))),
                            ^.className := "btn navbar-btn"))
        .build
    }
  }

  private val component = ReactComponentB[Unit]("Example")
    .initialState(State(Grid.component()))
    .renderBackend[Backend]
    .build

  def apply(): ReactElement = component()
}
