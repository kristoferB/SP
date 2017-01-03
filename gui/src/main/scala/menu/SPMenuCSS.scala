package spgui.menu

import scalacss.Defaults._

object SPMenuCSS extends StyleSheet.Inline {
  import dsl._

  val topNav = style(
    marginBottom(0.px), // ovverride bootstrap default
    backgroundColor(rgb(255, 102, 0)),
    border.none
  )

  val buttonList = style(
    marginBottom(0.px),
    paddingLeft(1.%%)      
  )

  // should probably be generalized. adding this so i can align it
  val dropDown = style(
   
  )
  val dropDownButton = style(
    backgroundColor(rgb(192,192,192))
  )

  this.addToDocument()
}
