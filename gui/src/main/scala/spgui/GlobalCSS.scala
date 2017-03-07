package spgui

import spgui.menu.SPMenuCSS

import scalacss.Defaults._

object GlobalCSS extends StyleSheet.Inline {
  import dsl._
  val defaultShadow = "0 2px 2px rgba(0, 0, 0, 0.05), 0 1px 0 rgba(0, 0, 0, 0.05)"

  val gridSpacing = "80px"
  val gridSpacingSmall = "20px"
  val gridColor = "#777777"
  val gridColorSmall = "#999999"
  val gridOffsetY = SPMenuCSS.topNavHeight

  val background = style(
    unsafeRoot("body")(
      backgroundColor.white
      /*backgroundImage :=
        s"linear-gradient(to right,  $gridColor 2px, transparent 0px)," +
        s"linear-gradient(to bottom, $gridColor 2px, transparent 0px),"  +
        s"linear-gradient(to right,  $gridColorSmall 2px, transparent 0px)," +
        s"linear-gradient(to bottom, $gridColorSmall 2px, transparent 0px)",
      backgroundSize :=
        s"$gridSpacing $gridSpacingSmall,"+
        s"$gridSpacingSmall $gridSpacing,"+
        s"$gridSpacingSmall  $gridSpacing",
      backgroundPosition:=s"0px $gridOffsetY"
      */
    )
  )
  val h2 = style(
    unsafeRoot("h2")(
      fontSize(30.px),
      lineHeight(40.px)
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

  
  val navbar = style(
    unsafeRoot(".navbar-nav > .open > a")(
      backgroundColor.transparent.important
    )
  )


  this.addToDocument()
}
