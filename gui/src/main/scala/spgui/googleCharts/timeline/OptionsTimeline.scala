/****************************************
  *      HELP CLASS TO GOOGLE CHARTS     *
  ****************************************/

package spgui.googleCharts.timeline

import scala.scalajs.js
import spgui.googleCharts.{OptionsTrait, Tooltips}

// Trait for Timeline Options
// children of OptionsTrait
trait OptionsTlTrait extends OptionsTrait {
  val avoidOverlappingGridLines:  Boolean
  val backgroundColor:            String
  val colors:                     js.Array[String]
  val enableInteractivity:        Boolean
  val fontName:                   String
  val fontSize:                   String
  val forceIFrame:                Boolean
  override val height:            Int
  val timeline:                   Timeline
  val tooltip:                    Tooltips
  override val width:             Int

  override def toDynamic():       js.Object
}

// OptionsTimeline Class
// see configuration options of timeline doucmentation
class OptionsTimeline (
                        override val height:                    Int,
                        override val width:                     Int,
                        override val timeline:                  Timeline,
                        override val tooltip:                   Tooltips,
                        // default values for TimelineAPI
                        override val avoidOverlappingGridLines: Boolean = true,
                        override val backgroundColor:           String = "white",
                        override val colors:                    js.Array[String] = null,
                        override val enableInteractivity:       Boolean = true,
                        override val fontName:                  String = "Arial",
                        override val fontSize:                  String = "automatic",
                        override val forceIFrame:               Boolean = false
                      )extends OptionsTlTrait {
  // auxilarry constructors
  def this(height: Int, width: Int, timeline: Timeline) =
    this(height, width, timeline, new Tooltips())
  def this(height: Int, width: Int, tooltip: Tooltips) =
    this(height, width, new Timeline(), tooltip)
  def this(height: Int, width: Int) =
    this(height, width, new Timeline(), new Tooltips())
  // empty constructor
  def this() = this(0,0)

  // implements method toDynamic()
  // argument: none
  // return a js.Object to match the google chart API for the options
  override def toDynamic(): js.Object =
    js.Dynamic.literal(
      avoidOverlappingGridLines = this.avoidOverlappingGridLines,
      backgroundColor = this.backgroundColor,
      colors = this.colors,
      enableInteractivity = this.enableInteractivity,
      fontName = this.fontName,
      fontSize = this.fontSize,
      forceIFrame = this.forceIFrame,
      height = this.height,
      timeline = js.Dynamic.literal(
        barLabelStyle = this.timeline.barLabelStyle,
        colorByRowLabel = this.timeline.colorByRowLabel,
        groupByRowLabel = this.timeline.groupByRowLabel,
        showBarLabels = this.timeline.showBarLabels,
        showRowLabels = this.timeline.showRowLabels,
        singleColor = this.timeline.singleColor,
        rowLabelStyle = this.timeline.rowLabelStyle
      ),
      tooltip = js.Dynamic.literal(
        isHtml = this.tooltip.isHtml,
        trigger = this.tooltip.trigger
      ),
      width = this.width
    )

  // overrides toString and print variables
  override def toString = s"OptionsTimeline($height, $width, $timeline, $tooltip, " +
    s"$avoidOverlappingGridLines, $backgroundColor, $colors, $enableInteractivity, " +
    s"$fontName, $fontSize, $forceIFrame)"
}