package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPNavbarElements{
  def button(text:String, onClick: Callback):Seq[TagMod] = Seq(
    <.a(text, ^.onClick --> onClick),
    ^.className := SPNavbarElementsCSS.clickable.htmlClass,
    ^.className := SPNavbarElementsCSS.button.htmlClass
  )
  
  def button(text:String, icon:ReactNode, onClick: Callback): Seq[TagMod] = Seq(
    <.a(text, icon, ^.onClick --> onClick),
    ^.className := SPNavbarElementsCSS.clickable.htmlClass,
    ^.className := SPNavbarElementsCSS.button.htmlClass
  )

  def button(icon:ReactNode, onClick: Callback): Seq[TagMod] = Seq(
    <.a(icon, ^.onClick --> onClick),
    ^.className := SPNavbarElementsCSS.clickable.htmlClass,
    ^.className := SPNavbarElementsCSS.button.htmlClass
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
      ^.className := SPNavbarElementsCSS.clickable.htmlClass
    ),
    <.ul(
      contents,
      ^.className := SPNavbarElementsCSS.dropDownList.htmlClass,
      ^.className := "dropdown-menu",
      aria.labelledby := "something"
    )
  )

}


