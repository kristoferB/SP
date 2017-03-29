package spgui.widgets


import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
  * Created by TheAmazingUnicorn on 2017-03-21.
  */
object Style extends StyleSheet.Inline {
  import dsl._
  val li = styleF.bool(selected => styleS(
    position.relative,
    display.block,
    width(160 px),
    padding(v = 10.px, h = 15.px),
    border :=! "1px solid #999999",
    cursor.pointer,
    fontWeight._500,
    mixinIfElse(selected)(color :=! "#555555", backgroundColor :=! "#A5C2EE")(
      backgroundColor.white,
      &.hover(color :=! "#555555", backgroundColor :=! "#A5C2EE"))
  ))

}
