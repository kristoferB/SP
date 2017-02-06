package spgui.widgets.examples

import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.SPWidgetComp

object Ping {
  def apply() = SPWidgetComp(spwb =>
    <.div(
      <.h3("Hello from Ping"),
      <.button(
        "Open Pong widget",
        ^.onClick --> spwb.openWidget("Pong")
      )
    )
  )
}

object Pong {
  def apply() = SPWidgetComp(spwb => <.h3("Hello from Pong"))
}
