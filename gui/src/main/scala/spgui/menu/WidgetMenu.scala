package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria
import scalacss.ScalaCssReact._

import spgui.circuit.{SPGUICircuit, AddWidget}
import spgui.WidgetList
import spgui.components.{ Dropdown, Icon }

object WidgetMenu {
  case class State(filterText: String)

  class Backend($: BackendScope[Unit, State]) {
    def addW(name: String, w: Int, h: Int): Callback =
      Callback(SPGUICircuit.dispatch(AddWidget(name, w, h)))
    def onFilterTextChange(e: ReactEventFromInput) =
      e.extract(_.target.value)(v => $.modState(_.copy(filterText = v)))

    def render(s: State) =
      Dropdown(
        <.div(
          Icon.chevronDown,
          "New widget"
        ),
        (<.div(
          ^.className := "input-group",
          <.input(
            ^.className := "form-control",
            ^.placeholder := "Find widget...",
            ^.aria.describedBy := "basic-addon1",
            ^.onChange ==> onFilterTextChange
          )
        ) ::
          WidgetList.list.collect{
            case w if (w._1.toLowerCase.contains(s.filterText.toLowerCase)) =>
              <.div(w._1, ^.onClick --> addW(w._1, w._3, w._4))
          }).toVdomArray
      )
  }

  private val component = ScalaComponent.build[Unit]("WidgetMenu")
    .initialState(State(""))
    .renderBackend[Backend]
    .build

  def apply() = component()
}
