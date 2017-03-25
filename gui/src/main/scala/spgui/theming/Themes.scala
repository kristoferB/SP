package spgui.theming

object Themes {
  def themeList: List[Theme] = List(
    Theme(
      // default: all variables unchanged
    ),

    Theme(
      name = "debug-red",
      borderless =  false,
      shadow = "1px 20px 20px rgba(0, 0, 0, 0.1), 0 10px 0 rgba(0, 0, 0, 0.1)" ,
      mainBackgroundColor= "#ff0000",
      widgetBackgroundColor ="#ff0000",
      widgetHeadingColor= "#ff0000",
      widgetHeadingIconColor= "#ff0000",
      navbarBackgroundColor= "#ff0000",
      navbarForegroundColor= "#ff0000",
      defaultTextColor= "#ff0000"
    ),

    Theme(
      name = "dark",
      borderless =  false,
      shadow = "1px 2px 2px rgba(0, 0, 0, 0.1), 0 1px 0 rgba(0, 0, 0, 0.1)" ,
      mainBackgroundColor= "#333333",
      widgetBackgroundColor ="#555555",
      widgetHeadingColor= "#1a1a1a",
      widgetHeadingIconColor= "#ffffff",
      navbarBackgroundColor= "#555555",
      defaultTextColor= "#000000"
    )
  )
}


