package spgui.widgets.charts

import scalacss.Defaults._

object ChartCSS extends StyleSheet.Inline {
  import dsl._

  val charts = style(
    backgroundColor(rgb(0, 180, 0))
  )

  this.addToDocument()
}
