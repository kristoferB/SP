package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPDropdown {
  case class Props(title: String, buttonsProps: List[(String, Callback)])

  val sampleProps = Props(
    "sample SPDropdown",
    ("hej1", Callback.log("hej1")) :: ("hej2", Callback.log("hej2")) :: Nil
  )

  private val component = ReactComponentB[Props]("SPDropdown")
    .render_P(props =>
    <.div(
      ^.className := "dropdown",
      <.div(
        ^.id := "something",
        ^.tpe := "button",
        ^.className := "btn navbar-btn",
        ReactAttr.Generic("data-toggle") := "dropdown",
        aria.haspopup := "true",
        aria.expanded := "false",
        props.title),
      <.ul(
        ^.className := "dropdown-menu",
        aria.labelledby := "something",
        for((text,cb) <- props.buttonsProps) yield
          <.li(<.a(text), ^.onClick --> cb)
      )
    )
  )
    .build

  def apply(title: String, buttonsProps: List[(String, Callback)]) =
    component(Props(title, buttonsProps))

  val example = component(sampleProps)
}
