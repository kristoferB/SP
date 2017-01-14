package spgui.circuit

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

// stuff below here doesn't belong to the circuit, just setting initial state to something
object SomeWidget {
  val component = ReactComponentB[Unit]("SomeWidget")
    .render(_ => <.h2("SomeWidget"))
    .build

  def apply() = component()
}
