package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPDropdown {
  private val theHtml = <.div(
    ^.className := "dropdown",
    <.button(
      ^.id := "something",
      ^.tpe := "button",
      ^.className := "btn dropdown-toggle",
      ReactAttr.Generic("data-toggle") := "dropdown",
      aria.haspopup := "true",
      aria.expanded := "false",
      "Dropdown example"),
    <.ul(
      ^.className := "dropdown-menu",
      aria.labelledby := "something",
      <.li(<.a("hej")),
      <.li(<.a("hej2"))
    )
  )

  private def component = ReactComponentB[Unit]("SPDropdown")
    .render(_ => theHtml)
    .build

  def apply() = component()
}
