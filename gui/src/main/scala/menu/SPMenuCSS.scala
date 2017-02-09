package spgui.menu

import scalacss.Defaults._

object SPMenuCSS extends StyleSheet.Inline {
  import dsl._

  val navbarBgColor = "#fff"
  val navbarFgColor = "#58666e"
  val navbar_header_background = "#de691c" //#df691a; //#f05050;   // this is the SP logo orange
  val navbar_brand_title_color = backgroundColor.white   // SP logo foreground white

  val defaultShadow = "0 2px 2px rgba(0, 0, 0, 0.05), 0 1px 0 rgba(0, 0, 0, 0.05)"

  val topNavHeight = 50.px //use in GlobalCSS.scala
  val topNav = style("sp-top-nav")(
    boxShadow := defaultShadow,
    marginBottom(0.px), // ovverride bootstrap default
    backgroundColor := navbarBgColor,
    color := navbarFgColor,
    border.none,
    display.table,
    width(100.%%),
    height(topNavHeight)
  )

  val buttonList = style("sp-button-list")(
    marginBottom(0.px),
    paddingLeft(1.%%)
  )

  val navbarCell = style("table-cell")(
    display.tableCell
  )


  val spLogo = style("spLogo")(
    backgroundRepeat := "no-repeat",
    backgroundImage := "url(images/splogo.svg)",
    width(70.px),
    borderWidth(5.px),
    borderStyle.inset,
    borderColor.transparent
  )

  // should probably be generalized. adding this so i can align it
  val dropDown = style("sp-dropwdown")(

  )

  this.addToDocument()
}
