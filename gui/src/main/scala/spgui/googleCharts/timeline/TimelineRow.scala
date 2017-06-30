 /****************************************
  *      HELP CLASS TO GOOGLE CHARTS     *
  ****************************************/

package spgui.googleCharts.timeline

import spgui.googleCharts.Tooltips

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
  // to create a new auxillary constructor
  // give it a new casesId and implement its else if() state in toArray()
  val cases:              Int
}

class TimelineRow (
                    override val rowLabel: String,
                    override val optionalBarLabel: String,
                    override val optionalTooltip: Tooltips,
                    override val startDate: Date,
                    override val endDate: Date,
                    override val cases: Int = 0
                  ) extends TimelineRowTrait {
  // constructor with rowLabel, dates and tooltip
  def this(rowLabel: String, optionalTooltip: Tooltips, startDate: Date, endDate: Date) =
    this(rowLabel, "", optionalTooltip, startDate, endDate, 1)

  // constructor with rowLabel and dates
  def this(rowLabel: String, startDate: Date, endDate: Date) =
    this(rowLabel, "", new Tooltips(), startDate, endDate, 2)

  // constructor only rowLabel displayed
  def this(rowLabel: String) =
    this(rowLabel, "", new Tooltips(), new js.Date(), new js.Date(), 3)

  // constructor missing tooltip
  def this(rowLabel: String, optionalBarLabel: String, startDate: Date, endDate: Date) =
    this(rowLabel, optionalBarLabel, new Tooltips(), startDate, endDate, 4)


  // TODO: ensure no side effects
  // argument: a TimelineRow
  // return: an js.Array of js.Any
  def toArray(): js.Array[js.Any] = {
    if (this.cases == 0) {
      js.Array(this.rowLabel, this.optionalBarLabel, this.optionalTooltip.toArray(), this.startDate, this.endDate)
    } else if (this.cases == 1) {
      js.Array(this.rowLabel, this.optionalTooltip.toArray(), this.startDate, this.endDate)
    } else if (this.cases == 2 || this.cases == 3) {
      js.Array(this.rowLabel, this.startDate, this.endDate)
    } else if (this.cases == 4) {
      js.Array(this.rowLabel, this.optionalBarLabel, this.startDate, this.endDate)
    } else {
      println("Something went wrong in TimelineRow.toArray()")
      new js.Array[js.Any]()
    }
  }


  override def toString = s"TimelineRow($rowLabel, $optionalBarLabel, $optionalTooltip, " +
    s"$startDate, $endDate, $cases)"
}