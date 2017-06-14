package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPNavbarElements{
  def button(text:String, onClick: Callback): VdomNode =
    <.a(text,
      ^.onClick --> onClick,
      ^.className := SPNavbarElementsCSS.clickable.htmlClass,
      ^.className := SPNavbarElementsCSS.button.htmlClass        
    )
  
  def button(text:String, icon:VdomNode, onClick: Callback): VdomNode =
    <.a(text,
      icon,
      ^.onClick --> onClick,
      ^.className := SPNavbarElementsCSS.clickable.htmlClass,
      ^.className := SPNavbarElementsCSS.button.htmlClass
    )
  def button(icon:VdomNode, onClick: Callback): VdomNode =
    <.a(icon,
      ^.onClick --> onClick,
      ^.className := SPNavbarElementsCSS.clickable.htmlClass,
      ^.className := SPNavbarElementsCSS.button.htmlClass
    )
  
  def dropdown(text: String, contents: Seq[TagMod]): VdomElement =
    <.li(
      <.a(
        text,
        Icon.caretDown,
        VdomAttr("data-toggle") := "dropdown",
        ^.id:="something",
        ^.className := "nav-link dropdown-toggle",
        aria.hasPopup := "true",
        aria.expanded := "false",
        ^.className := SPNavbarElementsCSS.clickable.htmlClass
      ),
      <.ul(
        contents.toTagMod,
        ^.className := SPNavbarElementsCSS.dropDownList.htmlClass,
        ^.className := "dropdown-menu",
        aria.labelledBy := "something"
      )
    )
}


