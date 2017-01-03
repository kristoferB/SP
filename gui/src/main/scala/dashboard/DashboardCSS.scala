package spgui.dashboard

import scalacss.Defaults._

object DashboardCSS extends StyleSheet.Inline {
  import dsl._

  val dashboardBackground = style("dashboard-background")(
  )

  val widgetPanel = style("widget-panel")(
    backgroundColor(rgb(192,192,192)),
    height(100.%%),
    marginBottom(0.px),
    overflow.hidden,

    borderRadius(0.px),
    borderWidth(1.px),
    borderColor(rgb(0,0,0))
  )

  val reactGridPlaceholder = style("react-grid-placeholder")(
    backgroundColor(rgb(255,102,0)),
    opacity(0.5)
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
  
