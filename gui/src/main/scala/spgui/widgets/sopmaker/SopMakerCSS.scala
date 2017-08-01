package spgui.widgets.sopmaker

import scalacss.Defaults._
import spgui.theming.Theming

object SopMakerCSS extends Theming.SPStyleSheet {
  import dsl._


  val sopComponent = style(
    overflow.visible.important
    //position.absolute
    //display.flex
  )

  this.addToDocument()

  class Position(x:Int, y:Int) extends Theming.SPStyleSheet {
    // slightly different way of adding styles
    // multiple instances of this style can (and will) exist at a given time
    val position = style(
      top(y.px),
      left(x.px)
    )

    this.addToDocument()
  }


}
