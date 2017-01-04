package spgui.widgets

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scala.concurrent.Future
import scala.concurrent.Promise

import fr.hmil.roshttp._

object WidgetCommTest {

  //type Comp = ReactComponentU[Unit, Unit, Unit, Element]
  case class State(str: String)

  private class Backend($: BackendScope[Unit, State]) {
    def changeState(str: String): Callback = $.setState(State(str))

    def render(s: State) =
      <.div(
        <.input(
          ^.value := s.str,
          ^.onChange ==> updateMe
        ),
        <.div(s.str),
        <.br(),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> send, "SEND"
        )
      )

    def updateMe(e: ReactEventI): Callback = {
      changeState(e.target.value)
    }

    def send(): Callback = {
      Callback.alert("CLICKED")
    }
  }


  private val component = ReactComponentB[Unit]("WidgetInjectionTest")
      .initialState(State("HEJ"))
    .renderBackend[Backend]
    .build

  def apply(): ReactElement = component()
}