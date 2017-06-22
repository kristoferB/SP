package spgui.theming

object Themes {
  def themeList: List[Theme] = List(
    Theme(
      // default: all variables unchanged
    ),

    Theme(
      name = "dark",
      borderless = false,
      defaultTextColor = "#ffffbb",
      mainBackgroundColor= "#333333",

      navbarBackgroundColor = "#111111",
      navbarForegroundColor = "#58666e",
      navbarLogoBackgroundColor = "#df691a",
      navbarButtonHoverColor = "#666666",

      widgetBackgroundColor = "#444444",
      widgetHeadingColor = "#555555",
      widgetHeadingIconColor = "#ffffff",
      
      buttonBorderColor = "#777777",
      buttonBackgroundColor = "#111111",
      buttonBackgroundColorHover = "#333333",
      buttonTextColor = "#ffffff"
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
      navbarLogoBackgroundColor= "#ff0000",
      defaultTextColor= "#ff0000"
    )
  )
}


