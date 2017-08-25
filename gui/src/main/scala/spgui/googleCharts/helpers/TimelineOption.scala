//package spgui.googleCharts.helpers
//
///**
//  * Created by alexa on 03/08/2017.
//  */
//
//import scalajs.js
//
//class TimelineOption(
//                      val height:   Int,
//                      val width:    Int,
//                      val timeline: TimelineInner,
//                      val tooltip:  Tooltips,
//                      val title:    String,
//                      // default values for TimelineAPI
//                      val avoidOverlappingGridLines: Boolean,
//                      val backgroundColor: String,
//                      val colors: js.Array[String],
//                      val enableInteractivity: Boolean,
//                      val fontName: String,
//                      val fontSize: String,
//                      val forceIFrame: Boolean
//                    ) {
//
//  def toDynamic: js.Object = js.Dynamic.literal(
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
//}
//
//object TimelineOption {
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
//           ) = new TimelineOption(
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
//           ) = new TimelineOption(
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
//           ) = new TimelineOption(
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
//           ) = new TimelineOption(
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
//           ) = new TimelineOption(
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
//           ) = new TimelineOption(
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
//             height: Int,
//             width: Int,
//             title: String
//           ) = new TimelineOption(
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
