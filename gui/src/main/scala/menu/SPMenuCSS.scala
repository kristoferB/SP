package spgui.menu

import scalacss.Defaults._

object SPMenuCSS extends StyleSheet.Inline {
  import dsl._

  val navbarBgColor = "#999999"
  val navbarFgColor = "#58666e"
  val navbar_header_background = "#999999" //#df691a;;   // this is the SP logo orange
  val navbar_brand_title_color = backgroundColor.white   // SP logo foreground white

  val topNavHeight = 50.px //use in GlobalCSS.scala
  val topNav = style("sp-top-nav")(
    marginBottom(0.px), // ovverride bootstrap default
    boxShadow := spgui.GlobalCSS.defaultShadow,
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
  val dropDownButton = style("sp-dropwdown")(
    display.flex
  )

  val buttonIconSpacing = style("sp-titled-dropdown")(
    paddingRight(10.px)
  )

  val untitledButton = style("sp-untitled-dropdown")(

  )

  this.addToDocument()
}
