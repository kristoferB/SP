package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalajs.js.Dynamic.{literal => l}

import spgui.SPWidget

object WidgetWithJSON {
  def apply() = SPWidget{spwb =>
    def onTextChange(e: ReactEventI): Callback =
      Callback(spwb.saveData(e.target.value))
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
}
