 /****************************************
  *      HELP CLASS TO GOOGLE CHARTS     *
  ****************************************/

package spgui.googleCharts.timeline

import spgui.googleCharts.general.Tooltips

import scala.scalajs.js
import scala.scalajs.js.Date


// trait for a timeline row
// Doc. Data format
// https://developers.google.com/chart/interactive/docs/gallery/timeline#data-format
trait TimelineRowTrait {
  val rowLabel:           String
  val optionalBarLabel:   String
  val optionalTooltip:    Tooltips
  val startDate:          Date
  val endDate:            Date
  // to make it more user-friendly with toArray
  // give it a new casesId and implement its else if() state in toArray()
  val cases:              Int

  def toArray: js.Array[js.Any]
}

class TimelineRow (
                    override val rowLabel: String,
                    override val optionalBarLabel: String,
                    override val optionalTooltip: Tooltips,
                    override val startDate: Date,
                    override val endDate: Date,
                    override val cases: Int = 0
                  ) extends TimelineRowTrait {

  // TODO: ensure no side effects
  // argument: a TimelineRow
  // return: an js.Array of js.Any
  def toArray: js.Array[js.Any] = {
    if (this.cases == 0) {
      js.Array(this.rowLabel, this.optionalBarLabel, this.optionalTooltip.toArray, this.startDate, this.endDate)
    } else if (this.cases == 1) {
      js.Array(this.rowLabel, this.optionalTooltip.toArray, this.startDate, this.endDate)
    } else if (this.cases == 2 || this.cases == 3) {
      js.Array(this.rowLabel, this.startDate, this.endDate)
    } else if (this.cases == 4) {
      js.Array(this.rowLabel, this.optionalBarLabel, this.startDate, this.endDate)
    } else {
      println("Something went wrong in TimelineRow.toArray()")
      new js.Array[js.Any]()
    }
  }


  override def toString: String = s"TimelineRow($rowLabel, $optionalBarLabel, $optionalTooltip, " +
    s"$startDate, $endDate, $cases)"
}

object TimelineRow {
  def apply(
             rowLabel:          String,
             optionalBarLabel:  String,
             optionalTooltip:   Tooltips,
             startDate:         Date,
             endDate:           Date,
             cases:             Int
           ) = new TimelineRow(
    rowLabel,
    optionalBarLabel,
    optionalTooltip,
    startDate,
    endDate,
    cases
  )

  def apply(
           rowLabel:          String,
           optionalBarLabel:  String,
           optionalTooltip:   Tooltips,
           startDate:         Date,
           endDate:           Date
           ) = new TimelineRow(
    rowLabel,
    optionalBarLabel,
    optionalTooltip,
    startDate,
    endDate
  )

  def apply(
             rowLabel:          String,
             optionalTooltip:   Tooltips,
             startDate:         Date,
             endDate:           Date
           ) = new TimelineRow(
    rowLabel,
    "",
    optionalTooltip,
    startDate,
    endDate,
    1
  )

  def apply(
             rowLabel:          String,
             optionalBarLabel:  String,
             startDate:         Date,
             endDate:           Date
           ) = new TimelineRow(
    rowLabel,
    optionalBarLabel,
    Tooltips(),
    startDate,
    endDate,
    4
  )

  def apply(
             rowLabel:          String,
             startDate:         Date,
             endDate:           Date
           ) = new TimelineRow(
    rowLabel,
    "",
    Tooltips(),
    startDate,
    endDate,
    2
  )
}