package spgui.widgets.sopmaker

import scalacss.DevDefaults._
import spgui.theming.Theming

object SopMakerCSS extends Theming.SPStyleSheet {
  import dsl._

  val noSelect = style(
    userSelect := "none"
  )

  val sopComponent = style(
    overflow.visible.important
    //position.absolute
    //display.flex
  )

  val opText = style(
    userSelect := "none"
  )

  this.addToDocument()
}
