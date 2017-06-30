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
  override var height:            Int
  val timeline:                   TimelineInner
  val tooltip:                    Tooltips
  override var width:             Int

  override def toDynamic():       js.Object
}

// OptionsTimeline Class
// see configuration options of timeline doucmentation
class OptionsTimeline (
                        override var height:                    Int,
                        override var width:                     Int,
                        override val timeline:                  TimelineInner,
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
  def this(height: Int, width: Int, timeline: TimelineInner) =
    this(height, width, timeline, new Tooltips())
  def this(height: Int, width: Int, tooltip: Tooltips) =
    this(height, width, new TimelineInner(null, true), tooltip)
  def this(height: Int, width: Int) =
    this(height, width, new TimelineInner(null, true), new Tooltips())
  // empty constructor
  // if we leave this to zero the API says it will
  // scale dynamicly but does not work for the height
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

  // set width and height
  def setWidth(width: Int): Unit =
    this.width = width
  def setHeight(height: Int): Unit =
    this.height = height

  // overrides toString and print variables
  override def toString = s"OptionsTimeline($height, $width, $timeline, $tooltip, " +
    s"$avoidOverlappingGridLines, $backgroundColor, $colors, $enableInteractivity, " +
    s"$fontName, $fontSize, $forceIFrame)"
}