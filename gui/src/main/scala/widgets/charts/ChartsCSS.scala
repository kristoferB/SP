package spgui.widgets.charts

import scalacss.Defaults._

object ChartCSS extends StyleSheet.Inline {
  import dsl._

  val charts = style(
    backgroundColor.rgb(0, 128, 0)
  )

  this.addToDocument()
}
