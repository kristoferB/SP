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
    fontSize(70.px),
    fontFamily :=! "Helvetica, Arial, sans-serif",
    fontWeight :=! "bold"
  )

  val widgetHeader = style(
    fontSize(23.px),
    paddingBottom(8.px),
    fontFamily :=! "Helvetica, Arial, sans-serif",
    fontWeight :=! "bold"
  )

  val widgetText = style(
    fontSize(22.px),
    paddingBottom(8.px),
    fontFamily :=! "Helvetica, Arial, sans-serif"
  )

  val helveticaZ = style(
      fontFamily :=! "Helvetica, Arial, sans-serif"
)


  }
