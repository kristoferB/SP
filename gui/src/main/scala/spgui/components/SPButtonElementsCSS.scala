package spgui.components

import scalacss.Defaults._

object SPButtonElementsCSS extends StyleSheet.Inline {
  import dsl._


  val bootstrapDropdownOverride = style(
    unsafeRoot(".dropdown-menu")(
      padding(8.px)
    )
  )

  val navButton = style(
    //backgroundColor(rgb(255,0,0))
  )

  val widgetButton = style(
    //backgroundColor(rgb(0,255,255))
  )

  val clickable = style(
   // cursor.pointer,
   // userSelect:= "none",
   // backgroundColor.transparent,
   // listStyle:= "none"//,

    //&.hover(
    //  backgroundColor:= "#ff0000"//"#ededed"
    //)
  )

  this.addToDocument()
}
