package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalajs.js.Dynamic.{literal => l}

import spgui.SPWidgetComp

object WidgetWithJSON {
  def apply() = SPWidgetComp{spwb =>
    def onTextChange(e: ReactEventI): Callback =
      spwb.saveData(l("ChangedWidgetWithJSONData" -> e.target.value))
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
