package spgui.dashboard

import scalacss.Defaults._
import spgui.circuit.SPGUICircuit

object DashboardCSS extends StyleSheet.Inline {
  import dsl._
  val theme = SPGUICircuit.zoom(_.settings.theme)

  val widgetBgColor = "#ededed"
  val widgetHeadingBg = "#999999"
  val widgetHeadingIconColor = "#ffffff"

  val widgetPanel = style("sp-widget-panel")(
    addClassName("panel panel-default"),
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

  val widgetPanelLabel = style(
    addClassName("modal-title"),
    float.left
  )

  val widgetPanelHidden = style(
    transitionDuration := "100ms",
    &.not(_.hover) (
      padding(0.px),
      opacity(0),
      height(8.px)
    )
  )

  val widgetPanelHeader = style(
    //padding(2.px),
    //display.block,
    //backgroundColor := theme.value.widgetHeadingColor,
    //color := theme.value.widgetHeadingColor

  )

  val widgetPanelButton = style(
    paddingLeft(4.px)
  )

  val widgetPanelBody = style("sp-panel-body")(
    backgroundColor := theme.value.widgetBackgroundColor,
    overflow.auto,
    height(100.%%)
  )

  val widgetPanelContent = style("sp-widget-panel-content")(
    backgroundColor := theme.value.widgetBackgroundColor,
    height(100.%%),
    padding(4.px)
  )

  val reactGridPlaceholder = style("react-grid-placeholder")(
    backgroundColor(rgb(255,102,0)),
    opacity(0.5)
  )

  val closeButton = style("close-button")(
    float.right,
    fontSize(15.px),
    padding(0.px, 7.px, 0.px, 0.px)
  )

  val overridPlaceholder = style(
    unsafeRoot(".react-grid-item.react-grid-placeholder ")(
      backgroundColor := "#df691a",
      opacity(0.3)
    )
  )

  this.addToDocument()
}
