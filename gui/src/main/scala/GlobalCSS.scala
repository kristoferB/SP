package spgui

import scalacss.Defaults._

object GlobalCSS extends StyleSheet.Inline {
  import dsl._

  val background = style(unsafeRoot("body")(
    backgroundColor:="#AAAAAA")
  )

  val layout = style(

  )

  this.addToDocument()
}
