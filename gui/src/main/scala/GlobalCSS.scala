package spgui

import spgui.menu.SPMenuCSS

import scalacss.Defaults._

object GlobalCSS extends StyleSheet.Inline {
  import dsl._

  val gridSpacing = 80.px
  val gridSpacingSmall = 20.px
  val gridColor = "#fff3"
  val gridColorSmall = "#fff3"
  val gridOffsetY = SPMenuCSS.topNavHeight

  val background = style(
    unsafeRoot("body")(
      backgroundColor.rgb(192,192,192),
      backgroundImage := "linear-gradient(" +gridColor +",2px, transparent 2px),"
        + "linear-gradient(90deg," +gridColor +",2px, transparent 2px),"
        + "linear-gradient(" +gridColorSmall +",1px, transparent 1px),"
        + "linear-gradient(90deg," +gridColorSmall +",1px, transparent 1px)",
      backgroundSize := gridSpacing.value +" " +gridSpacing.value +","
        +gridSpacing.value +" " +gridSpacing.value +","
        +gridSpacingSmall.value +" " +gridSpacingSmall.value +","
        +gridSpacingSmall.value +" " +gridSpacingSmall.value,
      backgroundPosition := 0 +" " +gridOffsetY.value
      //  background-position:-2px -2px, -2px -2px, -1px -1px, -1px -1px
      // TODO: this backgroundImage and Size options should be added, and maybe contributed to scalacss, they explicitly asks for it somewhere in the scalacss guide
    )
  )

  val button = style(
    unsafeRoot("button")(
      border.none,
      padding(10.px),
      borderWidth(1.px),
      borderColor.gray,
      borderStyle.solid
    )
  )

  val layout = style("sp-layout")(

  )

  this.addToDocument()
}
