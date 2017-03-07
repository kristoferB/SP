package spgui.components

import scalacss.Defaults._

object ComponentCSS extends StyleSheet.Inline {
  import dsl._
  val textColor = "#000000"

  val clickable = style(
    backgroundColor.transparent,
    &.hover(
      backgroundColor := "#999999"//"#ededed"
    )
  )

  // override bootstrap
  val navbar = style(
    unsafeRoot(".navbar-nav > .open > a")(
      (backgroundColor := "#999999").important
    )
  )

  val ovveride = style("hello")(
    backgroundColor.yellow
  )

  val text = style(
    color(rgb(0,0,0)).important
  )

  this.addToDocument()
}
