package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.circuit.{SPGUICircuit, AddWidget}
import spgui.WidgetList
import spgui.components.{ Icon, SPButtonElements, SPTextBox }

object WidgetMenuNew {
  case class State(filterText: String)

  class Backend($: BackendScope[Unit, State]) {
    def addW(name: String, w: Int, h: Int): Callback =
      Callback(SPGUICircuit.dispatch(AddWidget(name, w, h)))

    def render(s: State) =
      <.li(
        SPButtonElements.navbarDropdown(
          "New widget",
          List(
            SPTextBox(
              "Find widget...",
              (t: String) => { $.setState(State(filterText = t)) }
            ),
            WidgetList.list.collect{
              case e if (e._1.toLowerCase.contains(s.filterText.toLowerCase))=>
                <.div(
                  ^.onClick --> ( addW(e._1, e._3, e._4) ),
                  e._1
                )
            })
        ))
  }

  private val component = ReactComponentB[Unit]("WidgetMenu")
    .initialState(State(""))
    .renderBackend[Backend]
    .build

  def apply() = component()
}
