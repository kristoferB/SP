package spgui.menu

import scalacss.Defaults._

object SPMenuCSS extends StyleSheet.Inline {
  import dsl._

  val topNavHeight = 50.px //use in GlobalCSS.scala
  val topNav = style("sp-top-nav")(
    marginBottom(0.px), // ovverride bootstrap default
    backgroundColor.rgb(255, 102, 0),
    border.none,
    display.table,
    width(100.%%),
    height(topNavHeight) //the value should be entered here directly and then grabbed by the entry in GlobalCSS
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
  val dropDownButton = style("sp-dropdown-button")(
    backgroundColor.rgb(192,192,192)
  )

  this.addToDocument()
}
