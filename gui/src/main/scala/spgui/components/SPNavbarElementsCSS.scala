package spgui.components

import scalacss.Defaults._
import spgui.theming.SPStyleSheet

object SPNavbarElementsCSS extends SPStyleSheet {
  import dsl._

  val button = style(
    (color := theme.value.defaultTextColor).important,
    unsafeChild("span")(
      (color := theme.value.defaultTextColor).important
    ),
    &.active(
      //(backgroundColor:= "#aaaaaa").important
      //boxShadow:="inset 2px 2px 5px -1px rgba(0,0,0,0.2)"
    )
      //transition:= "box-shadow 0.1s"
  )

  val textIconClearance = style(marginRight(6.px) )

  val dropdownRoot = style (
    &.active(
      boxShadow:="inset 2px 2px 5px -1px rgba(0,0,0,0.2)"
    ),
    unsafeChild(".dropdown-menu")(
      padding(8.px)
    ),
    unsafeRoot(".navbar-dropdown.open > a > i")( 
      transform := "rotate(180deg)" // flip the caret
    ),
    unsafeRoot(".navbar-dropdown.open > a")(
      // overwrite a random color set by bootstrap
      (color := theme.value.defaultTextColor).important 
    )
  )

  val dropDownList = style(
    top.unset,
    cursor.pointer,
    backgroundColor:= theme.value.buttonBackgroundColor,
    color := theme.value.defaultTextColor
  )
  val clickable = style(
    (color := theme.value.defaultTextColor).important,
    cursor.pointer,
    userSelect:= "none",
    listStyle:= "none",
    &.hover(
      (backgroundColor :=! theme.value.navbarButtonHoverColor).important
    )
  )

  this.addToDocument()
}
