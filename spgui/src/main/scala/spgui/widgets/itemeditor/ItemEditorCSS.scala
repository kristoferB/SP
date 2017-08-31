package spgui.widgets.itemeditor

import scalacss.DevDefaults._

object ItemEditorCSS extends StyleSheet.Inline {
  import dsl._

  val dragZone = style("ace_text-input")(
    position.relative
  )

  val editor = style(
    width(100.%%),
    height(100.%%)
  )

  this.addToDocument()
}
