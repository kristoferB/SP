// Code
package spgui.widgets.charts

import scalacss.Defaults._

object TimelineCSS extends StyleSheet.Inline
{
  import dsl._

  val timelineStyle = style(
    backgroundColor.rgb(120,200,200)
  )

  this.addToDocument()
}