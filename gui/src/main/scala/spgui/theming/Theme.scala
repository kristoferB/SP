package spgui.theming

import scalacss.Defaults._

case class Theme(
  name: String = "default",
  borderless: Boolean = false,
  shadow: String = "1px 2px 2px rgba(0, 0, 0, 0.1), 0 1px 0 rgba(0, 0, 0, 0.1)",
  mainBackgroundColor: String = "#bbbbbb",
  defaultTextColor: String = "#000000",
  spOrange: String = "#df691a",

  navbarBackgroundColor: String = "#cccccc",
  navbarLogoBackgroundColor: String = "#df691a",
  navbarButtonBackgroundHoverColor: String = "#bbbbbb",
  navbarButtonTextColor: String = "#000000",

  widgetBackgroundColor: String = "#ededed",
  widgetHeadingColor: String = "#999999",
  widgetHeadingIconColor: String = "#ffffff",
  
  widgetButtonBorderColor: String = "#aaaaaa",
  widgetButtonBackgroundColor: String = "#dddddd",
  widgetButtonBackgroundHoverColor: String = "#ffffff",
  widgetButtonTextColor: String = "#000000"
)






