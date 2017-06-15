/**
  * Created by alexa on 15/06/2017.
  */
package spgui.widgets.charts

import scala.scalajs.js

@js.native
trait OptionsTooltip extends js.Object {
  val isHtml: Boolean = js.native
  val trigger: String = js.native
}

object OptionsTooltip {
  def apply(
             isHtml: Boolean,
             trigger: String
           ) = js.Dynamic.literal(
    isHtml = isHtml,
    trigger = trigger
  )
}

@js.native
trait OptionsTimeline extends js.Object {
  val barLabelStyle:     js.Object         = js.native
  val colorByRowLabel:   Boolean           = js.native
  val groupByRowLabel:   Boolean           = js.native
  val rowLabelStyle:     js.Object         = js.native
  val showBarLabels:     Boolean           = js.native
  val showRowLabels:     Boolean           = js.native
  val singleColor:       String            = js.native
}

object OptionsTimeline {
  def apply(
             barLabelStyle:    js.Object,
             colorByRowLabel:  Boolean,
             groupByRowLabel:  Boolean,
             rowLabelStyle:    js.Object,
             showBarLabels:    Boolean,
             showRowLabels:    Boolean,
             singleColor:      String
           ) = js.Dynamic.literal(
    barLabelStyle = barLabelStyle,
    colorByRowLabel = colorByRowLabel,
    groupByRowLabel = groupByRowLabel,
    rowLabelStyle = rowLabelStyle,
    showBarLabels = showRowLabels,
    showRowLabels = showRowLabels,
    singleColor = singleColor)
}

// Documentation:     https://developers.google.com/chart/interactive/docs/gallery/timeline#configuration-options
@js.native
trait Options extends js.Object {
  val avoidOverlappingGridLines:  Boolean           = js.native
  val backgroundColor:            String            = js.native
  val colors:                     js.Array[String]  = js.native
  val enableInteractivity:        Boolean           = js.native
  val fontName:                   String            = js.native
  val fontSize:                   String               = js.native
  val forceIFrame:                Boolean           = js.native
  val height:                     Int               = js.native
  val optionsTimeline:            OptionsTimeline   = js.native
  val optionsTooltip:             OptionsTooltip    = js.native
  val title:                      String            = js.native
  val width:                      Int               = js.native

}

object Options {
  def apply(
             title:                      String,
             height:                     Int,
             width:                      Int,

             // how to get colors and fontSize automatically???
             fontSize:                   String = "automatic",
             colors:                     js.Array[String] = null,

             avoidOverlappingGridLines:  Boolean   = true,
             backgroundColor:            String    = "white",
             enableInteractivity:        Boolean   = true,
             fontName:                   String    = "Arial",
             forceIFrame:                Boolean = false,
             optionsTimeline:            js.Object = OptionsTimeline(null, false, true, null, true, true, null),
             optionsTooltip:             js.Object = OptionsTooltip(true, "focus")
           ) = js.Dynamic.literal(
    avoidOverlappingGridLines = avoidOverlappingGridLines,
    backgroundColor = backgroundColor,
    colors = colors,
    enableInteractivity = enableInteractivity,
    fontName = fontName,
    fontSize = fontSize,
    forceIFrame = forceIFrame,
    height = height,
    optionsTimeline = optionsTimeline,
    optionsTooltip = optionsTooltip,
    title = title,
    width = width
  )
}
