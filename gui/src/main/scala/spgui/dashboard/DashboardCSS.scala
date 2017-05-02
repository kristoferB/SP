package spgui.dashboard

import scalacss.Defaults._

object DashboardCSS extends StyleSheet.Inline {
  import dsl._

  val widgetBgColor = "#ededed"
  val widgetHeadingBg = "#999999"
  val widgetHeadingIconColor = "#ffffff"

  val widgetPanel = style("sp-widget-panel")(
    backgroundColor :=! widgetBgColor,
    height(100.%%),
    marginBottom(0.px),
    overflow.hidden,
    display.flex,
    flexDirection.column,

    borderRadius(0.px),
    borderWidth(1.px),
    boxShadow := spgui.GlobalCSS.defaultShadow
  )

  val widgetPanelHeader = style("sp-widget-header")(
    padding(2.px),
    display.block,
    backgroundColor :=! widgetBgColor,
    color :=! widgetBgColor
  )

  val widgetPanelBody = style("sp-panel-body")(
    backgroundColor :=! widgetBgColor,
    overflow.auto,
    padding(3.px),
    height(100.%%)
  )

  val widgetPanelContent = style("sp-widget-panel-content")(
    backgroundColor :=! widgetBgColor,
    padding(3.px),
    height(100.%%)
  )

  val reactGridPlaceholder = style("react-grid-placeholder")(
    backgroundColor(rgb(255,102,0)),
    opacity(0.5)
  )

  val closeButton = style("close-button")(
    float.right,
    fontSize(2.px),
    padding(0.px, 7.px, 0.px, 0.px)
  )

  this.addToDocument()
}
