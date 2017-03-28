package spgui

import spgui.menu.SPMenuCSS

import scalacss.Defaults._

import spgui.circuit.SPGUICircuit
import diode._
import org.scalajs.dom.Console
import org.scalajs.dom.raw.Location

object GlobalCSS extends StyleSheet.Inline {
  import dsl._
  val defaultShadow = "1px 2px 2px rgba(0, 0, 0, 0.1), 0 1px 0 rgba(0, 0, 0, 0.1)"

  val gridSpacing = "80px"
  val gridSpacingSmall = "20px"
  val gridColor = "#777777"
  val gridColorSmall = "#999999"
  val gridOffsetY = SPMenuCSS.topNavHeight

  val theme = SPGUICircuit.zoom(_.settings.theme)
 
  var background = style(
    unsafeRoot("body")(
      backgroundColor := theme.value.mainBackgroundColor
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

  this.addToDocument()
}
