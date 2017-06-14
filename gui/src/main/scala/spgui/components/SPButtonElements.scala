package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPButtonElements{
  def navButton(text:String):Seq[TagMod] = Seq(
    <.a(text),
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := SPButtonElementsCSS.navButton.htmlClass
  )
  
  def navButton(text:String, icon:ReactNode): Seq[TagMod] = Seq(
    <.a(text, icon),
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := SPButtonElementsCSS.navButton.htmlClass
  )

  def navButton(icon:ReactNode): Seq[TagMod] = Seq(
    <.a(icon),
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := SPButtonElementsCSS.navButton.htmlClass
  )

  def widgetButton(text:String): Seq[TagMod] = Seq(
    <.a(text),
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := "btn btn-default",
    ^.className := SPButtonElementsCSS.widgetButton.htmlClass
  )

  def widgetButton(text:String, icon:ReactNode): Seq[TagMod] = Seq(
    <.a(text, icon),
    ^.className := "btn btn-default",
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := SPButtonElementsCSS.widgetButton.htmlClass
  )

  def widgetButton(icon: ReactNode): Seq[TagMod] = Seq(
    <.a(icon),
    ^.className := "btn btn-default",
    ^.className := SPButtonElementsCSS.clickable.htmlClass,
    ^.className := SPButtonElementsCSS.widgetButton.htmlClass
  )

  def navbarDropdown(text: String, contents: List[ReactNode]) = Seq(
    <.a(
      text,
      Icon.caretDown,
      ReactAttr.Generic("data-toggle") := "dropdown",
      ^.id:="something",
      ^.className := "nav-link dropdown-toggle",
      aria.haspopup := "true",
      aria.expanded := "false",
      ^.className := SPButtonElementsCSS.clickable.htmlClass
    ),
    <.ul(
      contents,
      ^.className := ComponentCSS.dropDownList.htmlClass,
      ^.className := "dropdown-menu",
      aria.labelledby := "something"
    )
  )

  def widgetDropdown(text: String, contents: List[ReactNode]) = Seq(
    <.a(
      text,
      Icon.caretDown,
      ReactAttr.Generic("data-toggle") := "dropdown",
      ^.id:="something",
      ^.className := "nav-link dropdown-toggle",
      aria.haspopup := "true",
      aria.expanded := "false",
      ^.className := "btn btn-default",
      ^.className := SPButtonElementsCSS.widgetButton.htmlClass,
      ^.className := SPButtonElementsCSS.clickable.htmlClass
    ),
    <.ul(
      contents,
      ^.className := ComponentCSS.dropDownList.htmlClass,
      ^.className := "dropdown-menu",
      aria.labelledby := "something"
    )
  )
}


