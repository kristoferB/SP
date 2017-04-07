package spgui.widgets.css

import scalacss.Defaults._

object WidgetStyles extends StyleSheet.Inline {
  import dsl._

  // val ff = fontFace("helveticaNeueRegular")(
  //   _.src("local(HelveticaNeue)", "url(font2.woff)")
  // ) //ARTO: dessa genererar @font-face


  val commonFont = mixin(
    //fontFamily(ff)
    fontSize(26.px),
    paddingLeft(10.px)
  )

  val clock = style(
    textAlign.center,
    fontSize(70.px)
    //fontFamily(ff)
  )

  val patientCardText = style(
    fontSize(26.px),
    svgFill := "black"
  )
  

  }
