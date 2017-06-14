package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPWidgetElements{
  def button(text:String, onClick: Callback): Seq[TagMod] = Seq(
    <.a(text, ^.onClick --> onClick),
    ^.className := SPWidgetElementsCSS.clickable.htmlClass,
    ^.className := "btn btn-default",
    ^.className := SPWidgetElementsCSS.button.htmlClass
  )

  def button(text:String, icon:ReactNode, onClick: Callback): Seq[TagMod] = Seq(
    <.a(text, icon, ^.onClick --> onClick),
    ^.className := "btn btn-default",
    ^.className := SPWidgetElementsCSS.clickable.htmlClass,
    ^.className := SPWidgetElementsCSS.button.htmlClass
  )

  def button(icon: ReactNode, onClick: Callback): Seq[TagMod] = Seq(
    <.a(icon, ^.onClick --> onClick),
    ^.className := "btn btn-default",
    ^.className := SPWidgetElementsCSS.clickable.htmlClass,
    ^.className := SPWidgetElementsCSS.button.htmlClass
  )

  def dropdown(text: String, contents: Seq[ReactNode]) = Seq(
    <.a(
      text,
      Icon.caretDown,
      ReactAttr.Generic("data-toggle") := "dropdown",
      ^.id:="something",
      ^.className := "nav-link dropdown-toggle",
      aria.haspopup := "true",
      aria.expanded := "false",
      ^.className := "btn btn-default",
      ^.className := SPWidgetElementsCSS.button.htmlClass,
      ^.className := SPWidgetElementsCSS.clickable.htmlClass
    ),
    <.ul(
      contents,
      ^.className := SPWidgetElementsCSS.dropDownList.htmlClass,
      ^.className := "dropdown-menu",
      aria.labelledby := "something"
    )
  )
}
