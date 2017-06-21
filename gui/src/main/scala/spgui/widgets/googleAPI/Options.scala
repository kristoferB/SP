/**
  * Created by alexa on 15/06/2017.
  */
package spgui.widgets.googleAPI

import scala.scalajs.js

/*
 * Timeline Doucmentation:
 * https://developers.google.com/chart/interactive/docs/gallery/timeline#configuration-options
 *
 * Full Documentation Google Charts
 * https://developers.google.com/chart/interactive/docs/reference#top_of_page
 */


/*
 * https://developers.google.com/chart/interactive/docs/customizing_tooltip_content
 * Tooltips are the little boxes that pop up when you hover over something
 */
@js.native
trait Tooltips extends js.Object {
  val isHtml: Boolean = js.native
  val trigger: String = js.native
}

// Facade for ToolTips
object Tooltips {
  def apply(
             isHtml: Boolean,
             trigger: String
           ) =
    js.Dynamic.literal(
      isHtml = isHtml,
      trigger = trigger
    )
}


// Configuration Options for Timeline
// Changing this
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

// See Timeline Docs
@js.native
trait Options extends js.Object {
  val avoidOverlappingGridLines:  Boolean           = js.native
  val backgroundColor:            String            = js.native
  val colors:                     js.Array[String]  = js.native
  val enableInteractivity:        Boolean           = js.native
  val fontName:                   String            = js.native
  val fontSize:                   String            = js.native
  val forceIFrame:                Boolean           = js.native
  val height:                     Int               = js.native
  val optionsTimeline:            OptionsTimeline   = js.native
  val tooltips:                   Tooltips          = js.native
  val width:                      Int               = js.native
}

// Facade for the Options
object Options {
  def apply(
             // height and width must be set
             height:                      Int,
             width:                       Int,
             // see full doc for default values
             // name:                     Type      = Default value
             avoidOverlappingGridLines:   Boolean   = true,
             backgroundColor:             String    = "white",
             colors:                      js.Array[String] = null,
             enableInteractivity:         Boolean   = true,
             fontName:                    String    = "Arial",
             fontSize:                    String = "automatic",
             forceIFrame:                 Boolean = false,
             optionsTimeline:             js.Object = OptionsTimeline(
               null, false, true, null, true, true, null
             ),
             tooltips:                    js.Object = Tooltips(true, "focus")
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
    tooltips = tooltips,
    width = width
  )
}
