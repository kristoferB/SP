package spgui.components

import scalacss.Defaults._
import spgui.theming.SPStyleSheet

object SPNavbarElementsCSS extends SPStyleSheet {
  import dsl._

  val bootstrapDropdownOverride = style(
    unsafeRoot("dropdown-menu")(
      padding(8.px)
    ),
    unsafeRoot("li.open > a > i")( // TODO dont use root, figure something out
      transform := "rotate(180deg)" // flip the caret
    ),
    unsafeRoot("li.open > a")(
      backgroundColor := "#000000 !important"
    )
  )

  val button = style(
    //backgroundColor(rgb(255,0,0))
  )

  val textIconClearance = style(marginRight(6.px) )

  val dropDownList = style(
    top.unset
  )
  val clickable = style(
    cursor.pointer,
    userSelect:= "none",
    //backgroundColor.transparent,
    listStyle:= "none",
    &.hover(
      backgroundColor(rgb(0,255,0)).important
    )
  )

  this.addToDocument()
}
