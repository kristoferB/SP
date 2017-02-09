package spgui.dashboard

import scalacss.Defaults._

object DashboardCSS extends StyleSheet.Inline {
  import dsl._

  val widgetBgColor = "#f5f5f5"

  val widgetPanel = style("sp-widget-panel")(
    backgroundColor(rgb(192,192,192)),
    height(100.%%),
    marginBottom(0.px),
    overflow.hidden,
    display.flex,
    flexDirection.column,

    borderRadius(0.px),
    borderWidth(1.px),
    borderColor(rgb(0,0,0))
  )

  val widgetPanelHeader = style("sp-widget-header")(
    padding(2.px),
    display.block,
    backgroundColor.rgb(224,224,224)
  )

  val widgetPanelBody = style("sp-panel-body")(
    backgroundColor := widgetBgColor,
    overflow.auto,
    height(100.%%)
  )

  val widgetPanelContent = style("sp-widget-panel-content")(
    backgroundColor := widgetBgColor,
    height(100.%%)
  )

  val reactGridPlaceholder = style("react-grid-placeholder")(
    backgroundColor(rgb(255,102,0)),
    opacity(0.5)
  )

  this.addToDocument()
}
