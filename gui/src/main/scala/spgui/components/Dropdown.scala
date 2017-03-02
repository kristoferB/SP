package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria

object Dropdown {
  case class Props(toggleButton: ReactNode, contents: Seq[ReactNode])

  private val component = ReactComponentB[Props]("Dropdown")
    .render_P(p =>
      <.div(
        ^.className := "dropdown",
        <.div(
          ^.id := "something",
          ^.tpe := "button",
          ReactAttr.Generic("data-toggle") := "dropdown",
          aria.haspopup := "true",
          aria.expanded := "false",
          p.toggleButton
        ),
        <.ul(
          ^.className := "dropdown-menu",
          aria.labelledby := "something",
          p.contents.map(c => <.li(<.a(c)))
        )
      )
    )
    .build

  def apply(toggleButton: ReactNode, contents: ReactNode*) =
    component(Props(toggleButton, contents.toSeq))
}
