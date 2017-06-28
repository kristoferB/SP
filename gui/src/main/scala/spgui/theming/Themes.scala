package spgui.theming

object Themes {
  def themeList: List[Theme] = List(
    Theme(
      // default: all variables unchanged
    ),

    Theme(
      name = "dark",
      borderless = false,
      defaultTextColor = "#ffffdd",
      mainBackgroundColor= "#222222",

      navbarBackgroundColor = "#222222",
      navbarLogoBackgroundColor = "#df691a",
      navbarButtonBackgroundHoverColor =  "#555555",
      navbarButtonTextColor  = "#ffffff",

      widgetBackgroundColor = "#333333",
      widgetHeadingColor = "#222222",
      widgetHeadingIconColor = "#ffffff",
      
      widgetButtonBorderColor = "#aaaaaa",
      widgetButtonBackgroundColor = "#222222",
      widgetButtonBackgroundHoverColor = "#333333",
      widgetButtonTextColor = "#ffffff"
    ),
    Theme(
      name = "light",
      borderless = false,
      defaultTextColor = "#000000",
      mainBackgroundColor= "#ffffff",

      navbarBackgroundColor = "#eeeeee",
      navbarLogoBackgroundColor = "#df691a",
      navbarButtonBackgroundHoverColor =  "#dddddd",
      navbarButtonTextColor  = "#000000",

      widgetBackgroundColor = "#f5f5f5",
      widgetHeadingColor = "#eeeeee",
      widgetHeadingIconColor = "#000000",
      
      widgetButtonBorderColor = "#ffffff",
      widgetButtonBackgroundColor = "#ffffff",
      widgetButtonBackgroundHoverColor = "#eeeeee",
      widgetButtonTextColor = "#000000"
    )
  )
}


