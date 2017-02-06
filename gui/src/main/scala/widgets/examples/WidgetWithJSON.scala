package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.circuit.SPGUICircuit
import spgui.circuit.SetWidgetData

import scalajs.js.JSON
import scalajs.js.Dynamic.{literal => l}

// TODO: make a trait or class Widget that is inherited by all widgets that
// sorts out the json-stringifying and keeps track of widgetIndex etc

object WidgetWithJSON {

  class Backend($: BackendScope[Int, Unit]) {
    def onTextChange(e: ReactEventI): Callback =
      Callback(SPGUICircuit.dispatch(SetWidgetData(0, JSON.stringify(l("WidgetWithJSONData" -> e.target.value)))))

    def render =
      <.div(
        <.h3("hello from WidgetWithJSON"),
        <.input(
          ^.tpe := "text",
          ^.defaultValue := "type something",
          ^.onChange ==> onTextChange
        ),
        "this text is stored in sessionStorage on every change, have a look in the console"
      )
  }

  private val component = ReactComponentB[Int]("WidgetWithJSON")
    .renderBackend[Backend]
    .build

  def apply(index: Int): ReactElement = component(index)
}
