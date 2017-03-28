package spgui.menu

import scalacss.Defaults._
import spgui.circuit.SPGUICircuit

object SPMenuCSS extends StyleSheet.Inline {
  import dsl._
  
  val topNavHeight = 50 //use in GlobalCSS.scala

  val splogoSVGHeight = 60.4
  val splogoSVGWidth = 170.1

  val theme = SPGUICircuit.zoom(_.settings.theme)
 
  val topNav = style("sp-top-nav")(
    display.flex,
    flexDirection.row,
    // ovverride bootstrap defaults
    marginBottom(0.px),
    border.none
  )

  val buttonPadding = 10
  val buttonList = style("sp-button-list")(
   paddingLeft(0.px)
  )

  val logoPadding = 0 // currently no padding needed
  val splogoHeight = topNavHeight * (1-logoPadding)
  val splogoWidth = splogoHeight * splogoSVGWidth / splogoSVGHeight
  val spLogo = style("spLogo")(
    backgroundRepeat := "no-repeat",
    backgroundImage := "url(images/splogo_title.svg)",
    height(splogoHeight.px),
    width(splogoWidth.px),
    marginLeft((logoPadding * splogoWidth / 2).px),
    marginRight((logoPadding * splogoWidth / 2).px)
  )

  val spLogoDiv = style("sp-logo-outer")(
    backgroundColor := theme.value.navbarLogoBackgroundColor,
    height.inherit,
    display.flex,
    alignItems.center
  )

  val splogoContainer = style("sp-splogo-container") (
    margin(0.px),
    padding(0.px),
    height(topNavHeight.px),
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

  val container = style("sp-navbar-container")(
    paddingLeft(0.px),
    backgroundColor := theme.value.navbarBackgroundColor,
    color := theme.value.defaultTextColor,
    border.none,
    display.flex,
    alignItems.center,
    width(100.%%),
    height(topNavHeight.px),
    position.relative
  )

  this.addToDocument()
}
