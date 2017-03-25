package spgui.dashboard

import scalacss.Defaults._
import spgui.circuit.SPGUICircuit 

object DashboardCSS extends StyleSheet.Inline {
  import dsl._
  val theme = SPGUICircuit.zoom(_.settings.theme)

  val widgetPanel = style("sp-widget-panel")(
    backgroundColor := theme.value.widgetBackgroundColor,
    height(100.%%),
    marginBottom(0.px),
    overflow.hidden,
    display.flex,
    flexDirection.column,

    borderRadius(0.px),
    borderWidth(0.px),
    boxShadow := theme.value.shadow
  )

  val widgetPanelHeader = style("sp-widget-header")(
    padding(2.px),
    display.block,
    backgroundColor := theme.value.widgetHeadingColor,
    color := theme.value.widgetHeadingColor
  )

  val widgetPanelBody = style("sp-panel-body")(
    backgroundColor := theme.value.widgetBackgroundColor,
    overflow.auto,
    height(100.%%)
  )

  val widgetPanelContent = style("sp-widget-panel-content")(
    backgroundColor := theme.value.widgetBackgroundColor,
    height(100.%%),
    padding(0.px)
  )

  val reactGridPlaceholder = style("react-grid-placeholder")(
    backgroundColor(rgb(255,102,0)),
    opacity(0.5)
  )

  val closeButton = style("close-button")(
    float.right,
    fontSize(15.px),
    padding(0.px, 7.px, 0.px, 0.px),
    color := theme.value.iconColor
  )

  val overridPlaceholder = style(
    unsafeRoot(".react-grid-item.react-grid-placeholder ")(
      backgroundColor := "#df691a",
      opacity(0.3)
    )
  )

  this.addToDocument()
}
