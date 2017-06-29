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
  val name:       String
  val id:         String
  val tooltip:    Tooltips
  val startDate:  Date
  val endDate:    Date
  val cases:      Int   // to make it more user-friendly with toArray
}

class TimelineRow (
                    override val name: String,
                    override val id: String,
                    override val tooltip: Tooltips,
                    override val startDate: Date,
                    override val endDate: Date,
                    override val cases: Int = 0
                  ) extends TimelineRowTrait {
  // constructor with name, dates and tooltip
  def this(name: String, tooltip: Tooltips, startDate: Date, endDate: Date) =
    this(name, "", tooltip, startDate, endDate, 1)

  // constructor with name and dates
  def this(name: String, startDate: Date, endDate: Date) =
    this(name, "", null, startDate, endDate, 2)

  // constructor only name displayed
  def this(name: String) =
    this(name, "", null, new js.Date(), new js.Date(), 3)

  // constructor missing tooltip
  def this(name: String, id: String, startDate: Date, endDate: Date) =
    this(name, id, null, startDate, endDate, 4)


  // TODO: ensure no side effects
  // argument: a TimelineRow
  // return: an js.Array of js.Any
  def toArray(): js.Array[js.Any] = {
    if (this.cases == 0) {
      js.Array(this.name, this.id, this.tooltip.toArray(), this.startDate, this.endDate)
    } else if (this.cases == 1) {
      js.Array(this.name, this.tooltip.toArray(), this.startDate, this.endDate)
    } else if (this.cases == 2 || this.cases == 3) {
      js.Array(this.name, this.startDate, this.endDate)
    } else if (this.cases == 4) {
      js.Array(this.name, this.id, this.startDate, this.endDate)
    } else {
      println("Something went wrong in TimelineRow.toArray()")
      new js.Array[js.Any]()
    }
  }


  override def toString = s"TimelineRow($name, $id, $tooltip, " +
    s"$startDate, $endDate, $cases)"
}