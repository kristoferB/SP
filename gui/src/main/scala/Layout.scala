package spgui

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom.document
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object Layout {

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
            MenuButton("Comp B", SomeOtherComp()))),
        s.component
      )

    object MenuButton {
      def apply(text: String, element: ReactElement): ReactElement =
        component(MenuBtnProps(text, element))
      case class MenuBtnProps(text: String, element: ReactElement)
      val component = ReactComponentB[MenuBtnProps]("MenuButton")
        .render_P(p => <.li(p.text,
                            ^.onClick --> changeState(p.element),
                            ^.className := "btn navbar-btn"))
        .build
    }
  }

  private val component = ReactComponentB[Unit]("Example")
    .initialState(State(Grid.component()))
    .renderBackend[Backend]
    .build

  val SomeOtherComp = ReactComponentB[Unit]("SomeOtherComp")
    .render_P(_ => <.h2("Hello from SomeOtherComp"))
    .build

  def apply(): ReactElement = component()
}
