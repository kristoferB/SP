//package spgui.googleCharts.helpers
//
//
//
//import scala.scalajs.js
//
///**
//  * Created by alexa on 03/08/2017.
//  */
//
//class TimelineRow(
//                   val rowLabel: String,
//                   val optionalBarLabel: String,
//                   val optionalTooltip: Tooltips,
//                   val startDate: js.Date,
//                   val endDate: js.Date,
//                   val cases: Int = 0
//                 ) {
//
//  // TODO: ensure no side effects
//  // argument: a TimelineRow
//  // return: an js.Array of js.Any
//  def toArray: js.Array[js.Any] = {
//    if (this.cases == 0) {
//      js.Array(this.rowLabel, this.optionalBarLabel, this.optionalTooltip.toArray, this.startDate, this.endDate)
//    } else if (this.cases == 1) {
//      js.Array(this.rowLabel, this.optionalTooltip.toArray, this.startDate, this.endDate)
//    } else if (this.cases == 2 || this.cases == 3) {
//      js.Array(this.rowLabel, this.startDate, this.endDate)
//    } else if (this.cases == 4) {
//      js.Array(this.rowLabel, this.optionalBarLabel, this.startDate, this.endDate)
//    } else {
//      println("Something went wrong in TimelineRow.toArray()")
//      new js.Array[js.Any]()
//    }
//  }
//}
//
//object TimelineRow {
//  def apply(
//             rowLabel: String,
//             optionalBarLabel: String,
//             optionalTooltip: Tooltips,
//             startDate: js.Date,
//             endDate: js.Date,
//             cases: Int
//           ) = new TimelineRow(
//    rowLabel,
//    optionalBarLabel,
//    optionalTooltip,
//    startDate,
//    endDate,
//    cases
//  )
//
//  def apply(
//             rowLabel: String,
//             optionalBarLabel: String,
//             optionalTooltip: Tooltips,
//             startDate: js.Date,
//             endDate: js.Date
//           ) = new TimelineRow(
//    rowLabel,
//    optionalBarLabel,
//    optionalTooltip,
//    startDate,
//    endDate
//  )
//
//  def apply(
//             rowLabel: String,
//             optionalTooltip: Tooltips,
//             startDate: js.Date,
//             endDate: js.Date
//           ) = new TimelineRow(
//    rowLabel,
//    "",
//    optionalTooltip,
//    startDate,
//    endDate,
//    1
//  )
//
//  def apply(
//             rowLabel: String,
//             optionalBarLabel: String,
//             startDate: js.Date,
//             endDate: js.Date
//           ) = new TimelineRow(
//    rowLabel,
//    optionalBarLabel,
//    Tooltips(),
//    startDate,
//    endDate,
//    4
//  )
//
//  def apply(
//             rowLabel: String,
//             startDate: js.Date,
//             endDate: js.Date
//           ) = new TimelineRow(
//    rowLabel,
//    "",
//    Tooltips(),
//    startDate,
//    endDate,
//    2
//  )
//}
