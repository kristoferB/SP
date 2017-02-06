package spgui.widgets.examples

import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.SPWidget

object Ping {
  def apply() = SPWidget(spwb =>
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
  def apply() = SPWidget(spwb => <.h3("Hello from Pong"))
}
