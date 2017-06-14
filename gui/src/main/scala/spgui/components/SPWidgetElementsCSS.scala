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
    //backgroundColor(rgb(255,0,0))
  )

  val dropDownList = style(
    top.unset
  )

  val clickable = style(
    cursor.pointer,
    userSelect:= "none",
    backgroundColor.transparent,
    listStyle:= "none"//,

    //&.hover(
    //  backgroundColor:= "#ff0000"//"#ededed"
    //)
  )

  this.addToDocument()
}
