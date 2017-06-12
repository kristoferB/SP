package spgui.components

import scalacss.Defaults._

object ComponentCSS extends StyleSheet.Inline {
  import dsl._
  val textColor = "#000000"

  // override bootstrap
  val navbar = style(
    unsafeRoot(".navbar-nav > .open > a")(
      (backgroundColor := "#999999").important
    )
  )

  val buttonStyle = style("sp-btn")(
    (padding(6.px,12.px, 6.px, 12.px)).important //There are a file somewhere overriding bootstrap defaults using ".nav > li > a"
  )

  val buttonCaret = style("btn-caret")(
      (marginLeft(4.px)).important
  )

  val ovveride = style("hello")(
    backgroundColor.yellow
  )

  val text = style(
    color(rgb(0,0,0)).important
  )

  this.addToDocument()
}
