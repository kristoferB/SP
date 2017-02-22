package spgui.widgets.itemexplorer

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object Style extends StyleSheet.Inline {
  import dsl._

  val ul = style(
    float.left,
    paddingLeft(0 px)
  )

  val li = styleF.bool(selected => styleS(
                         position.relative,
                         display.block,
                         width(160 px),
                         padding(v = 10.px, h = 15.px),
                         border :=! "1px solid #ecf0f1",
                         cursor.pointer,
                         fontWeight._500,
                         mixinIfElse(selected)(color :=! "#555555", backgroundColor :=! "#A5C2EE")(
                           backgroundColor.white,
                           &.hover(color :=! "#555555", backgroundColor :=! "#A5C2EE"))
                       ))

  val icon = style(
    float.left,
    paddingRight(10 px)
  )

  val chevron = style(float.right)

  this.addToDocument()
}
