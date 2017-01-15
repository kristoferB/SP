package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.circuit.SPGUICircuit

import scalajs.js.JSON
import scalajs.js.Dynamic.{literal => l}

// TODO: make a SetWidgetData(widgetIndex: Int, stringifiedJSON: String) Action in circuit
// and make this component dispatch that when textfield is changed
// then: make a trait or class Widget that is inherited by all widgets that
// sorts out the json-stringifying and keeps track of widgetIndex etc

object WidgetWithJSON {

  class Backend($: BackendScope[Int, Unit]) {
    def onTextChange(e: ReactEventI): Callback =
      Callback.alert(JSON.stringify(l("WidgetWithJSONData" -> e.target.value)))
    def render =
      <.div(
        <.h3("hello from WidgetWithJSON"),
        <.input(
          ^.tpe := "text",
          ^.value := "currVAl",
          ^.onChange ==> onTextChange
        )
      )
  }

  private val component = ReactComponentB[Int]("WidgetWithJSON")
    .renderBackend[Backend]
    .build

  def apply(index: Int): ReactElement = component(index)
}
