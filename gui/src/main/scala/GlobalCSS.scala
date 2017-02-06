package spgui

import scalacss.Defaults._

object GlobalCSS extends StyleSheet.Inline {
  import dsl._

  val background = style(
    unsafeRoot("body")(
      backgroundColor.rgb(192,192,192)
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
