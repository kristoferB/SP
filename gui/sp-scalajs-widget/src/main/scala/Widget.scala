package sp.widgets

import scala.scalajs.js.annotation.JSExport
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

@JSExport
class TestWidget {

  // entry point for the widget. 'Widget' is the root component of the widget
  // ComponentLoader will grab this specific widget using:
  // sp.widgets.TestWidget().getComponent

  // in the general case:
  // package.WidgetName().getComponent 

  // note that all the logic is somehow on the backend, despite this class
  // not existing on the backend at compile time. Seems to work just fine, somehow

  // TODO: make this component more interesting
  // also TODO: styling
  @JSExport
  def getComponent = RootComponent

  class RootBackend($: BackendScope[Unit, String]) {
    def handleChange(e: ReactEventI) =
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

  val RootComponent = ReactComponentB[Unit]("RootComponent")
    .initialState("RootComponent's input")
    .renderBackend[RootBackend]
    .build
    .reactClass

  class SubBackend($: BackendScope[Unit, String]) {
    def handleChange(e: ReactEventI) =
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

  val SubComponent = ReactComponentB[Unit]("SubComponent")
    .initialState("SubComponents's input = its title")
    .renderBackend[SubBackend]
    .build
}


