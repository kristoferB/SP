package spgui.widgets.sopmaker

import scalacss.Defaults._
import spgui.theming.Theming

object SopMakerCSS extends Theming.SPStyleSheet {
  import dsl._

  val noSelect = style(
    userSelect := "none"
  )

  val sopContainer = style(
    position.relative
  )

  val sopComponent = style(
    overflow.visible.important,
    touchAction:="none",
   // backgroundColor.red,
    position.absolute,
    zIndex := "1"
    //display.flex
  )

  val opText = style(
    userSelect := "none"
  )

  val dropZone = style(
    position.absolute,
    zIndex(100),
  //  backgroundColor.red,
    opacity:= "0.5"
  )
  val dropZoneOuter = style(
    position.absolute,
    zIndex(99),
    opacity:= "0.5"
  )

  val disableDropZone = style(
    pointerEvents := "none",
    visibility.hidden
  )

  val blue = style(
    backgroundColor:= "blue"
  )

  this.addToDocument()
}
