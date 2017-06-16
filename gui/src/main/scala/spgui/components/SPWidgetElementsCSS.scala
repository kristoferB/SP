package spgui.components

import scalacss.Defaults._

object SPWidgetElementsCSS extends StyleSheet.Inline {
  import dsl._

  // TODO move to a file containing all ovverides
  val bootstrapDropdownOverride = style(
    unsafeRoot(".dropdown-menu")(
      padding(8.px)
    )
  )

  val button = style(
    borderRadius(0.px)
  )

  val dropDownList = style(
    //top.unset
  )

  val defaultMargin = style(margin(2.px))

  val dropdownOuter = style(display.inlineBlock)
  val textIconClearance = style(marginRight(6.px) )

  val clickable = style(
    cursor.pointer,
    userSelect:= "none",
    //backgroundColor.transparent,
    listStyle:= "none"//,

    //&.hover(
    //  backgroundColor:= "#ff0000"//"#ededed"
    //)
  )

  this.addToDocument()
}
