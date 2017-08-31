package spgui

import spgui.menu.SPMenuCSS

import scalacss.DevDefaults._

import diode._
import org.scalajs.dom.Console
import org.scalajs.dom.raw.Location
import spgui.theming.Theming.SPStyleSheet

object GlobalCSS extends SPStyleSheet {
  import dsl._
  val defaultShadow = "1px 2px 2px rgba(0, 0, 0, 0.1), 0 1px 0 rgba(0, 0, 0, 0.1)"

  val gridSpacing = "80px"
  val gridSpacingSmall = "20px"
  val gridColor = "#777777"
  val gridColorSmall = "#999999"
  val gridOffsetY = SPMenuCSS.topNavHeight

  var background = style(
    unsafeRoot("body")(
      backgroundColor(_rgb(theme.value.mainBackgroundColor))
    )
  )

  val layout = style("sp-layout")(

  )

  this.addToDocument()
}
