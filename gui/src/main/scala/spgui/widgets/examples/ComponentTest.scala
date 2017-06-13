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
        <.div(
          ^.onClick --> Callback(testCallback("first test")) ,
          SPButtonElements.widgetButton("first test")
        ),
        <.div(
          ^.onClick --> Callback(testCallback("second test") ),
          SPButtonElements.widgetButton("second test", Icon.adjust)
        ),
        <.div(
          ^.onClick --> Callback(testCallback("! third test !") ),
          SPButtonElements.widgetButton(Icon.exclamation)
        ),
        <.div(
          ^.onClick --> Callback(testCallback("custom element test" )),
          Seq(<.button("custom element test"))
        ),
        
        SPButtonElements.dropdown(
          "dropdown",
          List(
            (<.div("hello0",^.onClick --> Callback(println("hello")))),
            (<.div("hello1", ^.onClick --> Callback(println(None)))),
            (<.div("hello2", ^.onClick --> Callback(println("triple, explicit hello"))))
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
