package spgui.widgets.componenttest

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import spgui.circuit.{SPGUICircuit, SetTheme}

import spgui.SPWidget
import spgui.components.Icon
import spgui.components.{SPWidgetElements => Comp}


object ComponentTest {
  private class MyBackend($: BackendScope[Unit, Unit]) {
    def render(p: Unit) =
      Comp.buttonGroup(Seq(
        Comp.button("first test", Callback(testCallback("first test"))),
        Comp.button("second test",Icon.ship, Callback(testCallback("second test"))),
        Comp.button(Icon.exclamation, Callback(testCallback("!!!!"))),
        Comp.dropdown(
          "dropdown",
          Seq(
            Comp.dropdownElement("hello 1", Callback(println("hello 1"))),
            Comp.dropdownElement("hello 2", Icon.ship, Callback(println("hello 2")))
          )
        ),
        Comp.TextBox(
          "I am the default text",
          (e => Callback(println("Text contents changed to: " + e)))
        )
      ))
  }

  private val component = ScalaComponent.builder[Unit]("Settings")
    .renderBackend[MyBackend]
    .build

  def testCallback(s:String) = {
    println(s)
  }

  def apply() = SPWidget(swpb => component())
}
