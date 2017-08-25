//package spgui.googleCharts.helpers
//
//import scala.scalajs.js
//
///**
//  * Created by alexa on 03/08/2017.
//  */
//
//class TimelineInner(
//                     val barLabelStyle: js.Object,
//                     val colorByRowLabel: Boolean,
//                     val groupByRowLabel: Boolean,
//                     val rowLabelStyle: js.Object,
//                     val showBarLabels: Boolean,
//                     val showRowLabels: Boolean,
//                     val singleColor: String
//                   ) {
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