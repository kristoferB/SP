package spgui.menu

import scalacss.Defaults._

object SPMenuCSS extends StyleSheet.Inline {
  import dsl._

  val navbarBgColor = "#999999"
  val navbarFgColor = "#58666e"
  val spOrange = "#df691a"
  val navbar_header_background = "#999999"
  val navbar_brand_title_color = backgroundColor.white 

  val topNavHeight = 50 //use in GlobalCSS.scala

  val splogoSVGHeight = 638.44
  val splogoSVGWidth = 960.05

  val topNav = style("sp-top-nav")(
    display.flex,
    flexDirection.row,
    // ovverride bootstrap defaults
    marginBottom(0.px),
    border.none
  )

  val buttonPadding = 10
  val buttonList = style("sp-button-list")(
   // marginBottom(0.px),
   // paddingLeft(0.px),
   // width.inherit,
   // verticalAlign.middle
  )

  val navbarCell = style("navbar-table-cell")(
    //display.tableCell
  )

  val logoPadding = 0.15
  val splogoHeight = topNavHeight * (1-logoPadding)
  val splogoWidth = splogoHeight * splogoSVGWidth / splogoSVGHeight
  val spLogo = style("spLogo")(
    backgroundRepeat := "no-repeat",
    backgroundImage := "url(images/splogo.svg)",
    height(splogoHeight.px),
    width(splogoWidth.px),
    marginLeft((logoPadding * splogoWidth / 2).px),
    marginRight((logoPadding * splogoWidth / 2).px)
  )

  val spLogoDiv = style("sp-logo-outer")(
    backgroundColor :=! spOrange,
    height.inherit,
    display.flex,
    alignItems.center
  )
  val splogoContainer = style("sp-splogo-container") (
    margin(0.px),
    padding(0.px),
    height(topNavHeight.px ),
    alignItems.center,
    display.flex
  )

  val dropDownButtonInner = style("sp-dropwdown-inner")(
    display.flex,
    marginLeft(6.px),
    marginRight(6.px)
  )

  val dropDownButton = style("sp-dropwdown")(
    margin(0.px),
    padding(3.px)
  )

  val buttonIconSpacing = style("sp-titled-dropdown")(
    paddingRight(10.px)
  )

  val container = style("sp-navbar-container")(
    paddingLeft(0.px),
    boxShadow := spgui.GlobalCSS.defaultShadow,
    backgroundColor :=! navbarBgColor,
    color :=! navbarFgColor,
    border.none,
    display.flex,
    alignItems.center,
    width(100.%%),
    height(topNavHeight.px),
    position.relative
  )

  this.addToDocument()
}
