package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria
import scalacss.ScalaCssReact._

import spgui.circuit.{SPGUICircuit, AddWidget}
import spgui.WidgetList
import spgui.components.{ Dropdown, Icon, SPDropdownNew }

object WidgetMenu {
  case class State(filterText: String)

  class Backend($: BackendScope[Unit, State]) {
    def addW(name: String, w: Int, h: Int): Callback =
      Callback(SPGUICircuit.dispatch(AddWidget(name, w, h)))
    def onFilterTextChange(e: ReactEventI) =
      e.extract(_.target.value)(v => $.modState(_.copy(filterText = v)))

    def render(s: State) =
      Dropdown("New widget", Seq(),
        <.div(
          ^.className := "input-group",
          <.input(
            ^.className := "form-control",
            ^.placeholder := "Find widget...",
            ^.aria.describedby := "basic-addon1",
            ^.onChange ==> onFilterTextChange
          )
        ) :: WidgetList.list.collect{
            case w if (w._1.toLowerCase.contains(s.filterText.toLowerCase)) =>
            <.div(w._1, ^.onClick --> addW(w._1, w._3, w._4))
          }: _*
      )
  }

  private val component = ReactComponentB[Unit]("WidgetMenu")
    .initialState(State(""))
    .renderBackend[Backend]
    .build

  def apply() = component()
}
