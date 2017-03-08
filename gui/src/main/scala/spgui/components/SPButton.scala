package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria
import spgui.circuit.{CloseAllWidgets, SPGUICircuit}

object CloseAllButton {
  case class Props()

  private val component = ReactComponentB[Props]("SPButton")
    .render_P(p =>
    <.li(
      ^.className := ComponentCSS.clickable.htmlClass,
      <.a(
        "Close all",
        ^.onClick -->  Callback(SPGUICircuit.dispatch(CloseAllWidgets)),
        ^.className := ComponentCSS.text.htmlClass

      )
    )
  )
    .build

  def apply() = component(Props())
}
