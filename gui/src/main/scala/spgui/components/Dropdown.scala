package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria

object Dropdown {
  case class Props(toggleButton: VdomNode, contents: Seq[VdomNode])

  private val component = ScalaComponent.builder[Props]("Dropdown")
    .render_P(p =>
    <.li(
      ^.className := ComponentCSS.clickable.htmlClass,
      ^.className := "dropdown",
      <.a(
        ^.className := ComponentCSS.text.htmlClass,
        ^.id := "something",
        ^.tpe := "button",
        VdomAttr("data-toggle") := "dropdown",
        aria.hasPopup := "true",
        aria.expanded := "false",
        p.toggleButton
      ),
      <.ul(
        ^.className := "dropdown-menu",
        aria.labelledBy := "something",
        p.contents.map(c => <.li(<.a(c))).toTagMod
      )
    )
  )
    .build

  def apply(toggleButton: VdomNode, contents: VdomNode*) =
    component(Props(toggleButton, contents.toSeq))
}
