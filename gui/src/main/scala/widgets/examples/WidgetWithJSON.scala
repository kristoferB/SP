package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalajs.js.Dynamic.{literal => l}

import spgui.SPWidget

object WidgetWithJSON extends SPWidget {
  def renderWidget =
    <.div(
      <.h3("hello from WidgetWithJSON"),
      <.input(
        ^.tpe := "text",
        ^.defaultValue := "type something",
        ^.onChange ==> onTextChange
      ),
      "this text is stored in sessionStorage on every change, have a look in the console"
    )

  def onTextChange(e: ReactEventI) = saveData(l("slightlyChangedWidgetWithJSONData" -> e.target.value))
}
