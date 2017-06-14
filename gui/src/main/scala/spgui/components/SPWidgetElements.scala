package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPWidgetElements{
  def button(text:String, onClick: Callback): VdomNode =
    <.a(text,
      ^.onClick --> onClick,
      ^.className := "btn btn-default",
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )
  
  def button(text:String, icon:VdomNode, onClick: Callback): VdomNode =
    <.a(
      text,
      icon,
      ^.onClick --> onClick,
      ^.className := "btn btn-default",
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )
  
  def button(icon: VdomNode, onClick: Callback): VdomNode =
    <.a(icon,
      ^.onClick --> onClick,
      ^.className := "btn btn-default",
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )
  

  def dropdown(text: String, contents: Seq[TagMod]) = Seq(
    <.a(
      text,
      Icon.caretDown,
      VdomAttr("data-toggle") := "dropdown",
      ^.id:="something",
      ^.className := "nav-link dropdown-toggle",
      aria.hasPopup := "true",
      aria.expanded := "false",
      ^.className := "btn btn-default",
      ^.className := SPWidgetElementsCSS.button.htmlClass,
      ^.className := SPWidgetElementsCSS.clickable.htmlClass
    ),
    <.ul(
      contents.toTagMod,
      ^.className := SPWidgetElementsCSS.dropDownList.htmlClass,
      ^.className := "dropdown-menu",
      aria.labelledBy := "something"
    )
  )
}
