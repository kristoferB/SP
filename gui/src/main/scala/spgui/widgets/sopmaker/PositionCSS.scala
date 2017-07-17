package spgui.widgets.sopmaker

import scalacss.Defaults._
import spgui.theming.Theming

class Position(x:Int, y:Int) extends Theming.SPStyleSheet {
  import dsl._

  val position = style(
    top(x.px),
    left(y.px)
  )

  this.addToDocument()
}
