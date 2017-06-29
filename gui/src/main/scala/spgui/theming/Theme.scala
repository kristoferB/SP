package spgui.theming

import scalacss.Defaults._

case class Theme(
  name: String = "default",
  borderless: Boolean = false,
  shadow: String = "1px 2px 2px rgba(0, 0, 0, 0.1), 0 1px 0 rgba(0, 0, 0, 0.1)",
  mainBackgroundColor: String = "#ffffff",
  widgetBackgroundColor: String = "#ededed",
  widgetHeadingColor: String = "#999999",
  widgetHeadingIconColor: String = "#ffffff",
  navbarBackgroundColor: String = "#eeeeee",
  navbarForegroundColor: String = "#58666e",
  navbarLogoBackgroundColor: String = "#df691a",
  defaultTextColor: String = "#000000",
  spOrange: String = "#df691a"
)
