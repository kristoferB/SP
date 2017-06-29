package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPWidgetElements{
  def button(text:String, onClick: Callback): VdomNode =
    <.button(text,
      ^.onClick --> onClick,
      ^.className := "btn btn-default",
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )
  
  def button(text:String, icon:VdomNode, onClick: Callback): VdomNode =
    <.button(
      <.span(text, ^.className:= SPWidgetElementsCSS.textIconClearance.htmlClass),
      icon,
      ^.onClick --> onClick,
      ^.className := "btn btn-default",
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )
  
  def button(icon: VdomNode, onClick: Callback): VdomNode =
    <.button(icon,
      ^.onClick --> onClick,
      ^.className := "btn btn-default",
      ^.className := SPWidgetElementsCSS.clickable.htmlClass,
      ^.className := SPWidgetElementsCSS.button.htmlClass
    )
  
  def dropdown(text: String, contents: Seq[TagMod]): VdomNode =
    <.span(
      ^.className:= SPWidgetElementsCSS.dropdownOuter.htmlClass,
      ^.className:= "dropdown",
      <.button(
        <.span(text, ^.className:= SPWidgetElementsCSS.textIconClearance.htmlClass),
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
