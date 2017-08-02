package spgui.widgets.sopmaker

import scalacss.Defaults._
import spgui.theming.Theming

object SopMakerCSS extends Theming.SPStyleSheet {
  import dsl._

  val hmm = style(
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
