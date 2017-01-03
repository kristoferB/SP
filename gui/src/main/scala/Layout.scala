package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.menu.SPMenu

object Layout {

  val contentConnection = AppCircuit.connect(_.content)

  case class State(component: ReactElement)

  class Backend($: BackendScope[Unit, State]) {
    def changeState(s: ReactElement): Callback = $.setState(State(s))
    def render(p: Unit, s: State) =
      <.div(
        SPMenu(),
        contentConnection(SPContentPane(_))
      )
  }

  private val component = ReactComponentB[Unit]("Example")
    .initialState(State(Grid.component()))
    .renderBackend[Backend]
    .build

  def apply(): ReactElement = component()
}
