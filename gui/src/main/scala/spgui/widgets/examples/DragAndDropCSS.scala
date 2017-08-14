package spgui.widgets.examples

import scalacss.DevDefaults._

object DragAndDropCSS extends StyleSheet.Inline {
  import dsl._

  val dragZone = style(
    backgroundColor(rgb(0,255,0)),
    width(100.px),
    height(100.px)
  )
  val dropZone = style(
    backgroundColor(rgb(255,0,0)),
    width(100.px),
    height(100.px)
  )

  this.addToDocument()
}
