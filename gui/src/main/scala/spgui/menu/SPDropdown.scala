package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPDropdown {
  case class Props(icon: ReactNode, buttonsProps: List[(String, Callback)])

  private val component = ReactComponentB[Props]("SPDropdown")
    .render_P(props =>
    <.div(
      ^.className := "dropdown",
      <.div(
        ^.id := "something",
        ^.tpe := "button",
        ^.className := "btn btn-default navbar-btn",
        ReactAttr.Generic("data-toggle") := "dropdown",
        aria.haspopup := "true",
        aria.expanded := "false",
        props.icon),
      <.ul(
        ^.className := "dropdown-menu",
        aria.labelledby := "something",
        for((text,cb) <- props.buttonsProps) yield
          <.li(<.a(text), ^.onClick --> cb)
      )
    )
  )
    .build

  def apply(buttonsProps: List[(String, Callback)],icon: ReactNode, title:String) =
    component(Props(
      <.div(
        ^.className := SPMenuCSS.dropDownButton.htmlClass,
        <.div(
          ^.className:= SPMenuCSS.buttonIconSpacing.htmlClass,
          icon
        ),
        title
      ),
      buttonsProps
    ))

  def apply(buttonsProps: List[(String, Callback)],title:String) =
    component(Props(title, buttonsProps))

  def apply(buttonsProps: List[(String, Callback)],icon: ReactNode) =
    component(Props(icon, buttonsProps))
}
