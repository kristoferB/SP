package spgui.widgets.charts

import scalacss.DevDefaults._

object GanttCSS extends StyleSheet.Inline {
  import dsl._

  val h = style(
    cursor.pointer
  )

  val background = style(
    backgroundColor(rgb(0,180,50))
  )

  this.addToDocument()
}

