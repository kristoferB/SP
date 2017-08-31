package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import spgui.SPWidget
import sp.domain._

object WidgetWithJSON {
  def apply() = SPWidget{spwb =>
    def onTextChange(e: ReactEventFromInput): Callback = Callback(spwb.updateWidgetData(SPValue(e.target.value)))

    val text = if (spwb.getWidgetData == SPValue.empty) "some text" else spwb.getWidgetData.asOpt[String].getOrElse("")

    <.div(
      <.h3("hello from WidgetWithJSON"),
      <.input(
        ^.tpe := "text",
        ^.defaultValue := text,
        ^.onChange ==> onTextChange
      ),
      "this text is stored in sessionStorage on every change, have a look in the console"
    )
  }
}
