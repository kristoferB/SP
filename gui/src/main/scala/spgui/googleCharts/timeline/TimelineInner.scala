///****************************************
//  *      HELP CLASS TO GOOGLE CHARTS     *
//  ****************************************/
//
//package spgui.googleCharts.timeline
//
//import scala.scalajs.js
//
//// Documentation: See Configuration Options for Timeline
//
//// Configuration Options for Timeline inner
//// trait
//trait TimelineInnerTrait {
//  val barLabelStyle:     js.Object
//  val colorByRowLabel:   Boolean
//  val groupByRowLabel:   Boolean
//  val rowLabelStyle:     js.Object
//  val showBarLabels:     Boolean
//  val showRowLabels:     Boolean
//  val singleColor:       String
//}
//
//// null, false, true, null, true, true, null
//class TimelineInner (
//                           // defaults values from API
//                           override val barLabelStyle: js.Object,
//                           override val colorByRowLabel: Boolean,
//                           override val groupByRowLabel: Boolean,
//                           override val rowLabelStyle: js.Object,
//                           override val showBarLabels: Boolean,
//                           override val showRowLabels: Boolean,
//                           override val singleColor: String
//                         ) extends TimelineInnerTrait {
//
//  def toArray: js.Array[js.Any] =
//    js.Array(
//      this.barLabelStyle,
//      this.colorByRowLabel,
//      this.groupByRowLabel,
//      this.rowLabelStyle,
//      this.showBarLabels,
//      this.showRowLabels,
//      this.singleColor
//    )
//
//
//  override def toString: String = s"Timeline($barLabelStyle, " +
//    s"$colorByRowLabel, $groupByRowLabel, " +
//    s"$rowLabelStyle, $showBarLabels, " +
//    s"$showRowLabels, $singleColor)"
//}
//
//object TimelineInner {
//  def apply(
//             barLabelStyle: js.Object,
//             colorByRowLabel: Boolean,
//             groupByRowLabel: Boolean,
//             rowLabelStyle: js.Object,
//             showBarLabels: Boolean,
//             showRowLabels: Boolean,
//             singleColor: String
//           ) = new TimelineInner(
//    barLabelStyle,
//    colorByRowLabel,
//    groupByRowLabel,
//    rowLabelStyle,
//    showBarLabels,
//    showRowLabels,
//    singleColor
//  )
//
//  def apply() =
//    new TimelineInner(
//      // default values
//      null, // barLabelStyle
//      false, // colorByRowLabel
//      true, // groupByRowLabel
//      null, // rowLabelStyle
//      true, // showBarLabels
//      true, // showRowLabels
//      null // singleColor
//    )
//}