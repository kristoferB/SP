package spgui.widgets.componenttest

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import spgui.circuit.{SPGUICircuit, SetTheme}

import spgui.SPWidget

import spgui.components._

object ComponentTest {
  private class MyBackend($: BackendScope[Unit, Unit]) {
    def render(p: Unit) =
      <.div(
        SPWidgetElements.button("first test", Callback(testCallback("first test"))),
        SPWidgetElements.button("second test",Icon.ship, Callback(testCallback("second test"))),
        SPWidgetElements.button(Icon.exclamation, Callback(testCallback("!!!!"))),        
        SPWidgetElements.dropdown(
          "dropdown",
          List(
            (<.li("hello0",^.onClick --> Callback(println("hello")))),
            (<.li("hello1", ^.onClick --> Callback(println(None)))),
            (<.li("hello2", ^.onClick --> Callback(println("triple, explicit hello"))))
          )
        )
      )
  }

  private val component = ScalaComponent.builder[Unit]("Settings")
    .renderBackend[MyBackend]
    .build

  def testCallback(s:String) = {
    println(s)
  }

  def apply() = SPWidget(swpb => component())
}
