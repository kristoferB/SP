package spgui.circuit

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

// stuff below here doesn't belong to the circuit, just setting initial state to something
object PlaceholderComp {
  val component = ReactComponentB[Unit]("PlaceholderComp")
    .render(_ => <.h2("placeholder"))
    .build

  def apply() = component()
}

object SomeWidget {
  val component = ReactComponentB[Unit]("SomeWidget")
    .render(_ => <.h2("SomeWidget"))
    .build

  def apply() = component()
}
