package spgui.dashboard

import scalacss.Defaults._

object DashboardCSS extends StyleSheet.Inline {
  import dsl._

  val dashboardBackground = style("dashboard-background")(
    backgroundColor := "#AAAAAA",
    height := "100%",
    flexGrow:="1"
  )

  val widgetPanel = style("widget-panel")(
    backgroundColor:= "#CCCCCC",
    height := "100%",
    borderRadius := "0"
  )

  this.addToDocument()
}


/*
  val bacon = style("food-bacon")(
    backgroundColor.pink
  )

  val eggs = style("food-eggs")(
    backgroundColor.black
  )

  val bold = style("bold-text")(
    fontWeight.bold
  )

  // Keyframe animation example
  val baconAnimation = keyframes(
    (0 %%) -> eggs,
    (50 %%) -> bacon,
    (100 %%) -> eggs
  )

  val baconAndEggs = style("eggs-bacon")(
    animationDuration := "1s",
    animationIterationCount.infinite,
    animationName(baconAnimation)
  )
 */
  
