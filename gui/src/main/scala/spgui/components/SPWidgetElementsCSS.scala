package spgui.components

import scalacss.Defaults._
import spgui.theming.SPStyleSheet

object SPWidgetElementsCSS extends SPStyleSheet {
  import dsl._

  val button = style(
    borderRadius(0.px),
    borderColor := theme.value.buttonBorderColor,
    backgroundColor := theme.value.buttonBackgroundColor,
    color := theme.value.buttonTextColor
  )

  val dropDownList = style(
    borderRadius(0.px)
  )

  val defaultMargin = style(margin(2.px))

  val dropdownRoot = style (
    unsafeChild(".dropdown-menu")(
      padding(8.px)
    ),
    unsafeChild("span.open > button > i")(
      transform := "rotate(180deg)" // flip the caret
    ),
    unsafeChild("span.open > button")(
      borderColor := theme.value.spOrange,
      backgroundColor := theme.value.buttonBackgroundColorHover
    )
  )

  val dropdownOuter = style(
    display.inlineBlock
  )

  val textIconClearance = style(marginRight(6.px) )

  val clickable = style(
    cursor.pointer,
    userSelect:= "none",
    //backgroundColor.transparent,
    listStyle:= "none",
    
    &.hover (
      borderColor := theme.value.spOrange,
      backgroundColor := theme.value.buttonBackgroundColorHover
    )
  )

  val textBox = style(
    &.focus(
      boxShadow := "inset 0px 0px 0px #000000",
      border := "1px solid " + theme.value.spOrange
      //transition := "border-color ease-in-out 0s, box-shadow ease-in-out 0s"
    )
  )

  val hidden = style(
    visibility.hidden
  )

  this.addToDocument()
}
