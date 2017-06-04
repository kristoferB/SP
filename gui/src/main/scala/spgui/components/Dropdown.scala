package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria

object Dropdown {
  case class Props(
    label:String,
    tags: Option[Seq[TagMod]] = None,
    contents: Seq[VdomNode])

  private val component = ScalaComponent.builder[Props]("Dropdown")
    .render_P(p =>
        <.div(SPButton(p.label,
          <.span(^.className:="caret " +ComponentCSS.buttonCaret.htmlClass ),
          Seq(^.id:="something",
            ^.className := "nav-link dropdown-toggle",
            VdomAttr("data-toggle") := "dropdown",
            aria.hasPopup := "true",
            aria.expanded := "false")
          ),
        <.ul(
          ^.className := "dropdown-menu",
          aria.labelledBy := "something",
          p.contents.map(c => <.li (<.a( ^.className:= "dropdown-item", ^.href:="#", c))).toTagMod
        ))
    )
    .build

  def apply(label:String, tags:Seq[TagMod], contents: VdomNode*) =
    component(Props(label, Some(tags), contents.toSeq ))

  // def apply(label:String, contents: VdomNode*) =
  //   component(Props(label, None, contents.toSeq))

}
