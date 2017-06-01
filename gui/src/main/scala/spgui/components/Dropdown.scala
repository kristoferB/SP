package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria

object Dropdown {
  case class Props(
    label:String,
    tags: Option[Seq[TagMod]] = None,
    contents: Seq[ReactNode])

  private val component = ReactComponentB[Props]("Dropdown")
    .render_P(p =>
        <.div(SPButton(p.label,
          <.span(^.className:="caret " +ComponentCSS.buttonCaret.htmlClass ),
          Seq(^.id:="something",
            ^.className := "nav-link dropdown-toggle",
            ReactAttr.Generic("data-toggle") := "dropdown",
            aria.haspopup := "true",
            aria.expanded := "false")
          ),
        <.ul(
          ^.className := "dropdown-menu",
          aria.labelledby := "something",
          p.contents.map(c => <.li (<.a( ^.className:= "dropdown-item", ^.href:="#", c)))
        ))
    )
    .build

  def apply(label:String, tags:Seq[TagMod], contents: ReactNode*) =
    component(Props(label, Some(tags), contents.toSeq ))

  // def apply(label:String, contents: ReactNode*) =
  //   component(Props(label, None, contents.toSeq))

}
