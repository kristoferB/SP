package spgui.dashboard

import scalacss.DevDefaults._
import spgui.theming.Theming.SPStyleSheet

object DashboardCSS extends SPStyleSheet {
  import dsl._

  val widgetBgColor = "#ededed"
  val widgetHeadingBg = "#999999"
  val widgetHeadingIconColor = "#ffffff"

  val widgetPanel = style("sp-widget-panel")(
    color(_rgb(theme.value.defaultTextColor)).important,
    addClassName("panel panel-default"),
    backgroundColor(_rgb(theme.value.widgetBackgroundColor)),
    height(100.%%),
    marginBottom(0.px),
    overflow.hidden,
    display.flex,
    flexDirection.column,

    borderRadius(0.px),
    borderWidth(0.px),
    boxShadow := theme.value.shadow,

    transitionDuration:=!"300ms",
    unsafeChild("i")(
      (textShadow:= "none").important,
      transitionDuration:=!"300ms"
    ),
    unsafeChild("h5")(
      transitionDuration:=!"300ms"
    )
  )


  val widgetPanelLabel = style(
    addClassName("modal-title"),
    float.left
  )

  val widgetPanelHidden = style(
    height(0.px),
    paddingTop(0.px),
    paddingBottom(0.px),
  
    unsafeChild("i")(
      color(_rgba(theme.value.defaultTextColor, 0)).important,
      transitionDuration:=!"300ms",
      visibility.hidden
    ),
    unsafeChild("h5")(
      color(_rgba(theme.value.defaultTextColor, 0)).important,
      transitionDuration:=!"300ms",
      visibility.hidden
    )
  )

  val widgetPanelHeader = style(
    transitionDuration:=!"300ms",
    userSelect := "none"
  )

  val widgetPanelButton = style(
    paddingLeft(4.px),
    transitionDuration:=!"300ms",
    color(_rgb(theme.value.defaultTextColor)).important
  )

  val widgetPanelBody = style("sp-panel-body")(
    backgroundColor(_rgb(theme.value.widgetBackgroundColor)),
    overflow.auto,
    height(100.%%)
  )

  val widgetPanelContent = style("sp-widget-panel-content")(
    backgroundColor(_rgb(theme.value.widgetBackgroundColor)),
    height(100.%%),
    padding(4.px)
  )

  val closeButton = style("close-button")(
    float.right,
    fontSize(15.px),
    padding(0.px, 7.px, 0.px, 0.px)
  )

  val overridPlaceholder = style(
    unsafeRoot(".react-grid-item.react-grid-placeholder ")(
      backgroundColor :=! "#df691a",
      opacity(0.3)
    )
  )

  this.addToDocument()
}
