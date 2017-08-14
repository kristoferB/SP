package spgui.components

import scalacss.DevDefaults._
import spgui.theming.Theming

object SPNavbarElementsCSS extends Theming.SPStyleSheet {
  import dsl._

  val button = style(
    (color ( _rgb(theme.value.defaultTextColor))).important,
    unsafeChild("span")(
      (color ( _rgb(theme.value.defaultTextColor))).important
    ),
    &.active(
      //(backgroundColor:= "#aaaaaa").important
      //boxShadow:="inset 2px 2px 5px -1px rgba(0,0,0,0.2)"
    )
      //transition:= "box-shadow 0.1s"
  )

  val textIconClearance = style(marginRight(6.px) )

  val dropdownRoot = style (
    unsafeChild(".dropdown-menu")(
      padding(4.px)
    ),
    unsafeRoot(".navbar-dropdown.open > a > i")( 
      transform := "rotate(180deg)" // flip the caret
    ),
    unsafeRoot(".navbar-dropdown.open > a")(
      // bootstrap override
      color(_rgb(theme.value.defaultTextColor)).important
    ),
    unsafeRoot(".navbar-default .navbar-nav > .open > a")(
      // another bootstrap override
      backgroundColor(_rgb(theme.value.navbarBackgroundColor))
    )
  )


  val dropDownList = style(
    top.unset,
    cursor.pointer,
    backgroundColor(_rgb(theme.value.navbarBackgroundColor)).important,
    color(_rgb(theme.value.defaultTextColor))
  )

  val clickable = style(
    color(_rgb(theme.value.defaultTextColor)).important,
    cursor.pointer,
    userSelect := "none",
    listStyle := "none",
    &.hover(
      backgroundColor(_rgb(theme.value.navbarButtonBackgroundHoverColor)).important
    )
  )

  val dropdownElement = style(
    padding(4.px),
    &.hover(
      backgroundColor(_rgb(theme.value.navbarButtonBackgroundHoverColor)).important
    )
  )

  this.addToDocument()
}
