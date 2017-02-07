package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.SPWidget

object Ping {
  def apply() = SPWidget(spwb =>
    <.div(
      <.h3("Hello from Ping"),
      <.button(
        "Open Pong widget",
        ^.onClick --> spwb.openWidget("Pong")
      ),
      <.button(
        "Send message to Pongs",
        ^.onClick --> spwb.publish("hej där")
      )
    )
  )
}

object Pong {
  def apply() = SPWidget{spwb =>
    spwb.subscribe(s =>
      Callback.log("widget of id " + spwb.id + " got a msg saying " + s))
    <.h3("Hello from Pong")
  }
}
