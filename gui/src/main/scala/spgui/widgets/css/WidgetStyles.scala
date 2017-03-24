package spgui.widgets.css

import scalacss.Defaults._


object WidgetStyles extends StyleSheet.Inline {
  import dsl._

  val ff = fontFace("AlteHaasGrotesk")(
    _.src("local(./fonts/AlteHaasGroteskRegular.ttf)")
  ) //ARTO: dessa genererar @font-face

  val ff2 = fontFace("Dosis")(
    _.src("url(https://fonts.gstatic.com/s/dosis/v6/yeSIYeveYSpVN04ZbWTWghTbgVql8nDJpwnrE27mub0.woff2)")
    .fontWeight._600
    .fontStyle.normal
  )

  val clock = style(
    textAlign.center,
    fontSize(70.px),
    fontFamily(ff)
  )

  // val cirkel = style(
  //   fill :=! "green"
  // )

}
