package spgui.menu

import scalacss.Defaults._

object SPMenuCSS extends StyleSheet.Inline {
  import dsl._

  val topNav = style("sp-top-nav")(
    marginBottom(0.px), // ovverride bootstrap default
    backgroundColor.rgb(255, 102, 0),
    border.none
  )

  val buttonList = style("sp-button-list")(
    marginBottom(0.px),
    paddingLeft(1.%%)      
  )

  // should probably be generalized. adding this so i can align it
  val dropDown = style("sp-dropwdown")(
   
  )
  val dropDownButton = style("sp-dropdown-button")(
    backgroundColor.rgb(192,192,192)
  )

  this.addToDocument()
}
