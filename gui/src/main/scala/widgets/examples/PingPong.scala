package spgui.widgets.examples

import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.SPWidget

object Ping extends SPWidget {
  def renderWidget =
    <.div(
      <.h3("Hello from Ping"),
      <.button(
        "Open Pong widget",
        ^.onClick --> openWidget("Pong")
      )
    )
}

object Pong extends SPWidget {
  def renderWidget = <.h3("Hello from Pong")
}
