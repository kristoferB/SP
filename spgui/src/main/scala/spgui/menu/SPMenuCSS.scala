package spgui.menu

import scalacss.DevDefaults._
import spgui.circuit.SPGUICircuit
import spgui.theming.Theming.SPStyleSheet

object SPMenuCSS extends SPStyleSheet {
  import dsl._

  val topNavHeight = 50 //use in GlobalCSS.scala

  val splogoSVGHeight = 60.4
  val splogoSVGWidth = 170.1

  val topNav = style("sp-top-nav")(
    marginBottom(0.px),
    border.none
  )

  val navbarContents = style(
    border.none,
    backgroundColor(_rgb(theme.value.navbarBackgroundColor))
  )

  val topNavHeader = style(
  )

  val navbarToggleButton = style(
    fontSize(20.px),
    height(50.px),
    width(50.px),
    margin.unset,
    borderRadius.unset,
    border.none,
    textAlign.center,
    padding.unset,
    color(_rgb( theme.value.spOrange)),
    backgroundColor.transparent,
    position.relative
  )


  val navbarToggleButtonIcon = style(
    position.absolute,
    top(50.%%),
    left(50.%%),
    transform := "translate(-50%,-50%)"
  )

  val navbarToggleButtonOuter = style(

   )

  val buttonPadding = 4;
  val navItem = style("sp-nav-item")(
    paddingRight(buttonPadding.px),
    height(100.%%)
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
    backgroundColor(_rgb(theme.value.navbarLogoBackgroundColor)),
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
    backgroundColor(_rgb(theme.value.navbarBackgroundColor)),
    color(_rgb(theme.value.defaultTextColor)),
    border.none,
    display.flex,
    flexDirection.row,
    alignItems.center,
    width(100.%%),
    height(topNavHeight.px),
    position.relative
  )

  val expandButtonOverride = style(
    unsafeRoot(".navbar-default .navbar-toggle:hover, .navbar-default .navbar-toggle:focus")(
      backgroundColor(_rgb(theme.value.spOrange)),
      color(_rgb(theme.value.navbarBackgroundColor))
    )
  )

  this.addToDocument()
}
