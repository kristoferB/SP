package sp.widgets

import scala.scalajs.js.annotation.JSExport
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

@JSExport
class TestWidget {

  @JSExport
  def getComponent = RootComponent

  class RootBackend($: BackendScope[Unit, String]) {
    def handleChange(e: ReactEventFromInput) =
      $.setState(e.target.value)

    def clearAndFocusInput() =
      $.setState("")

    def render(state: String) =
      <.div(
        <.div(
          ^.onClick --> clearAndFocusInput,
          "This text is static content in TestWidget"
        ),
        <.input(
          ^.value     := state,
          ^.onChange ==> handleChange
        ),
        SubComponent()
      )
  }

  val RootComponent = ScalaComponent.builder[Unit]("RootComponent")
    .initialState("RootComponent's input")
    .renderBackend[RootBackend]
    .build
    .reactClass

  class SubBackend($: BackendScope[Unit, String]) {
    def handleChange(e: ReactEventFromInput) =
      $.setState(e.target.value)

    def clearAndFocusInput() =
      $.setState("")

    def render(state: String) =
      <.div(
        <.div(
          ^.onClick --> clearAndFocusInput,
          state),
        <.input(
          ^.value     := state,
          ^.onChange ==> handleChange))
  }

  val SubComponent = ScalaComponent.builder[Unit]("SubComponent")
    .initialState("SubComponents's input = its title")
    .renderBackend[SubBackend]
    .build
}


