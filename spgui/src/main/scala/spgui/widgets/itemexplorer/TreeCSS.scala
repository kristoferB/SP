package spgui.widgets.itemexplorer

import scalacss.DevDefaults._
import scalacss.ScalaCssReact._

object Style extends StyleSheet.Inline {
  import dsl._

  val itemContent = style(
//    &.hover(
//      cursor.pointer
//    )
  )

  val outerDiv = style(
    //backgroundColor.white,
    //display.inlineBlock,
    //minWidth(400 px),
    //minHeight(400 px)
  )

  val optionBar = style(
    position.relative,
    display.flex,
    flexDirection.row
  )

  val treeDiv = style(
    display.flex,
    flexDirection.row,
    float.left
  )

  val tvColumn = style(
    display.flex,
    flexDirection.row,
    padding(0 px)
  )

  val ul = style(
    padding(0 px)
  )

  val li = styleF.bool(selected => styleS(
                         position.relative,
                         display.block,
                         width(160 px),
                         padding(v = 10.px, h = 15.px),
                         border :=! "1px solid #999999",
                         cursor.pointer,
                         fontWeight._500,
                         mixinIfElse(selected)(color :=! "#555555", backgroundColor :=! "#A5C2EE")(
                           backgroundColor.white
//                           ,
//                           &.hover(color :=! "#555555", backgroundColor :=! "#A5C2EE"))
                         )
                       ))

  val icon = style(
    float.left,
    paddingRight(10 px)
  )

  val chevron = style(float.right)

  this.addToDocument()
}
