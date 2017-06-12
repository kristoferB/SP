package spgui.widgets.componenttest

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spgui.circuit.{SPGUICircuit, SetTheme}

import spgui.SPWidget

import spgui.components._

object ComponentTest {
  private class MyBackend($: BackendScope[Unit, Unit]) {
    def render(p: Unit) =
      <.div(
        // SPClickable(
        //   SPButtonElements.widgetButton("hello"),
        //   Callback(testCallback("lols"))
        // ),
        // SPClickable(
        //   SPButtonElements.widgetButton("hello", Icon.adjust),
        //   Callback(testCallback("lols"))
        // ),
        // SPClickable(
        //   SPButtonElements.widgetButton(Icon.exclamation),
        //   Callback(testCallback("lols"))
        // ),
        // SPClickable(
        //   Seq(<.button("custom element created on the spot")),
        //   Callback(testCallback("custom element"))
        // ),
        SPDropdownNew(
          SPButtonElements.widgetButton("dropdown", Icon.caretDown),
          List(
            //SPClickable(<.div("hello0"), Callback(testCallback("hello"))),
            //SPClickable("hello1", Callback(None)),
            //SPClickable("hello2", Callback(println("triple, explicit hello")))
          )
        )
      )
  }

  private val component = ReactComponentB[Unit]("Settings")
    .renderBackend[MyBackend]
    .build

  def testCallback(s:String) = {
    println(s)
  }

  def apply() = SPWidget(swpb => component())
}
