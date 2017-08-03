///****************************************
//  *      HELP CLASS TO GOOGLE CHARTS     *
//  ****************************************/
//
//package spgui.googleCharts.timeline
//
//import scala.scalajs.js
//import spgui.googleCharts.general.{OptionsTrait, Tooltips, TooltipsTrait}
//
//// Trait for Timeline Options
//// children of OptionsTrait
//trait OptionsTlTrait extends OptionsTrait {
//  val avoidOverlappingGridLines:  Boolean
//  val backgroundColor:            String
//  val colors:                     js.Array[String]
//  val enableInteractivity:        Boolean
//  val fontName:                   String
//  val fontSize:                   String
//  val forceIFrame:                Boolean
//  override val height:            Int
//  val timeline:                   TimelineInner
//  val title:                      String
//  val tooltip:                    Tooltips
//  override val width:             Int
//
//  def toDynamic:       js.Object
//}
//
//// OptionsTimeline Class
//// see configuration options of timeline documentation
//class OptionsTimeline (
//                             override val height:                    Int,
//                             override val width:                     Int,
//                             override val timeline:                  TimelineInner,
//                             override val tooltip:                   Tooltips,
//                             override val title:                     String,
//                             // default values for TimelineAPI
//                             override val avoidOverlappingGridLines: Boolean,
//                             override val backgroundColor:           String,
//                             override val colors:                    js.Array[String],
//                             override val enableInteractivity:       Boolean,
//                             override val fontName:                  String,
//                             override val fontSize:                  String,
//                             override val forceIFrame:               Boolean
//                           )extends OptionsTlTrait {
//  // implements method toDynamic()
//  // argument: none
//  // return a js.Object to match the google chart API for the options
//  def toDynamic: js.Object =
//  js.Dynamic.literal(
//    avoidOverlappingGridLines = this.avoidOverlappingGridLines,
//    backgroundColor = this.backgroundColor,
//    colors = this.colors,
//    enableInteractivity = this.enableInteractivity,
//    fontName = this.fontName,
//    fontSize = this.fontSize,
//    forceIFrame = this.forceIFrame,
//    height = this.height,
//    timeline = js.Dynamic.literal(
//      barLabelStyle = this.timeline.barLabelStyle,
//      colorByRowLabel = this.timeline.colorByRowLabel,
//      groupByRowLabel = this.timeline.groupByRowLabel,
//      showBarLabels = this.timeline.showBarLabels,
//      showRowLabels = this.timeline.showRowLabels,
//      singleColor = this.timeline.singleColor,
//      rowLabelStyle = this.timeline.rowLabelStyle
//    ),
//    title = this.title,
//    tooltip = js.Dynamic.literal(
//      isHtml = this.tooltip.isHtml,
//      trigger = this.tooltip.trigger
//    ),
//    width = this.width
//  )
//
//  // overrides toString and print variables
//  override def toString: String = s"OptionsTimeline($height, $width, $timeline, $tooltip, " +
//    s"$avoidOverlappingGridLines, $backgroundColor, $colors, $enableInteractivity, " +
//    s"$fontName, $fontSize, $forceIFrame)"
//}
//
//object OptionsTimeline {
//  def apply(
//             height:                    Int,
//             width:                     Int,
//             timeline:                  TimelineInner,
//             tooltip:                   Tooltips,
//             title:                     String,
//             avoidOverlappingGridLines: Boolean,
//             backgroundColor:           String,
//             colors:                    js.Array[String],
//             enableInteractivity:       Boolean,
//             fontName:                  String,
//             fontSize:                  String,
//             forceIFrame:               Boolean
//           ) = new OptionsTimeline(
//    height, width, timeline, tooltip, title,
//    avoidOverlappingGridLines, backgroundColor,
//    colors, enableInteractivity, fontName,
//    fontSize, forceIFrame
//  )
//
//  def apply(
//             height: Int,
//             width: Int,
//             timeline: TimelineInner,
//             tooltip: Tooltips,
//             title: String
//           ) = new OptionsTimeline(
//    height, width, timeline, tooltip, title,
//    // default values for the other variables (see Config-data at Google Timeline-Docs)
//    true, //avoidOverlappingGridLines
//    "white", // backgroundColor
//    null, // colors
//    true, // enableInteractivity
//    "Arial", // fontName
//    "automatic", // fontSize
//    false // forceIFrame
//  )
//
//  def apply(
//             height: Int,
//             width: Int,
//             timeline: TimelineInner,
//             title: String
//           ) = new OptionsTimeline(
//    height, width, timeline,
//    // default values for the other variables (see Config-data at Google Timeline-Docs)
//    Tooltips(), // tooltip
//    title, // EXCEPT TITLE
//    true, //avoidOverlappingGridLines
//    "white", // backgroundColor
//    null, // colors
//    true, // enableInteractivity
//    "Arial", // fontName
//    "automatic", // fontSize
//    false // forceIFrame
//  )
//
//  def apply(
//             height: Int,
//             width: Int,
//             tooltip: Tooltips,
//             title: String
//           ) = new OptionsTimeline(
//    height, width,
//    TimelineInner(), // timelineInner
//    tooltip, // tooltip
//    title, // title
//    true, //avoidOverlappingGridLines
//    "white", // backgroundColor
//    null, // colors
//    true, // enableInteractivity
//    "Arial", // fontName
//    "automatic", // fontSize
//    false // forceIFrame
//
//  )
//
//  def apply(
//             height: Int,
//             width: Int,
//             timeline: TimelineInner
//           ) = new OptionsTimeline(
//    height, width, timeline,
//    // default values for the other variables (see Config-data at Google Timeline-Docs)
//    Tooltips(), // tooltip
//    "NoTitleProvided", // title
//    true, //avoidOverlappingGridLines
//    "white", // backgroundColor
//    null, // colors
//    true, // enableInteractivity
//    "Arial", // fontName
//    "automatic", // fontSize
//    false // forceIFrame
//  )
//
//  def apply(
//             height: Int,
//             width: Int,
//             tooltip: Tooltips
//           ) = new OptionsTimeline(
//    height, width,
//    TimelineInner(), // timelineInner
//    tooltip, // tooltip
//    "NoTitleProvided", // title
//    true, //avoidOverlappingGridLines
//    "white", // backgroundColor
//    null, // colors
//    true, // enableInteractivity
//    "Arial", // fontName
//    "automatic", // fontSize
//    false // forceIFrame
//
//  )
//
//  def apply(
//              height: Int,
//              width: Int,
//              title: String
//           ) = new OptionsTimeline(
//    height, width,
//    TimelineInner(),
//    Tooltips(),
//    title,
//    true, //avoidOverlappingGridLines
//    "white", // backgroundColor
//    null, // colors
//    true, // enableInteractivity
//    "Arial", // fontName
//    "automatic", // fontSize
//    false // forceIFrame
//  )
//}
