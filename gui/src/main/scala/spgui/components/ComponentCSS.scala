package spgui.components

import scalacss.Defaults._

object ComponentCSS extends StyleSheet.Inline {
  import dsl._
  val textColor = "#000000"

  val dropDownList = style(
    top.unset
  )

  // override bootstrap
  // val navbar = style(
  //   unsafeRoot(".navbar-nav > .open > a")(
  //     (backgroundColor := "#999999").important
  //   )
  // )

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
