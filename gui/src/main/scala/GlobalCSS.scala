package spgui

import scalacss.Defaults._

object GlobalCSS extends StyleSheet.Inline {
  import dsl._

  val widgetPanel = style("widget-panel")(
    backgroundColor:= "#CCCCCC",
    height := "100%",
    borderRadius := "0"
  )

  this.addToDocument()
}
