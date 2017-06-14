package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria
import scalacss.ScalaCssReact._

import spgui.circuit.{SPGUICircuit, AddWidget}
import spgui.WidgetList
import spgui.components.{ Icon, SPNavbarElements, SPTextBox }

object WidgetMenu {
  case class State(filterText: String)

  class Backend($: BackendScope[Unit, State]) {
    def addW(name: String, w: Int, h: Int): Callback =
      Callback(SPGUICircuit.dispatch(AddWidget(name, w, h)))

    def render(s: State) =
      SPNavbarElements.dropdown(
        "New widget",
        
          // TODO: elementize sptextbox and put it back here
          // SPTextBox(
          //   "Find widget...",
          //   (t: String) => { $.setState(State(filterText = t)) }
          // )

          WidgetList.list.collect{
            case e if (e._1.toLowerCase.contains(s.filterText.toLowerCase))=>
              <.div(
                ^.onClick --> ( addW(e._1, e._3, e._4) ),
                e._1
              )
          }
        )
      
  }

  private val component = ScalaComponent.builder[Unit]("WidgetMenu")
    .initialState(State(""))
    .renderBackend[Backend]
    .build

  def apply() = component()
}
