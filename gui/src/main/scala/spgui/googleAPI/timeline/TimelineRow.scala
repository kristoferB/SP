/**
  * Created by alexa on 21/06/2017.
  */
package spgui.googleAPI.timeline

import spgui.googleAPI.Tooltips

import scala.scalajs.js
import scala.scalajs.js.Date

// Doc. Data format
// https://developers.google.com/chart/interactive/docs/gallery/timeline#data-format

trait TimelineRowTrait {
  val name:       String
  val id:         String
  val tooltips:   Tooltips
  val startDate:  Date
  val endDate:    Date
}

/*
object TimelineRow {
  def apply(
             id: String,
             name: String,
             tooltips: Tooltips,
             startDate: Date,
             endDate: Date
           ) = js.Dynamic.literal(
    id = id,
    name = name,
    tooltips = tooltips,
    startDate = startDate,
    endDate = endDate
  )
}*/


class TimelineRow (
                    override val name: String,
                    override val id: String,
                    override val startDate: Date,
                    override val endDate: Date,
                    override val tooltips: Tooltips = new Tooltips()
                  ) extends TimelineRowTrait {


  // return a array of type js.any
  // argument: that of type TimelineRow
  /*def toArray(that: TimelineRow): js.Array[js.Any] =
    Array(""+ that.name, ""+ that.id, ""+ that.tooltips, that.startDate, that.endDate)
*/
  def toArray(that: TimelineRow): js.Array[js.Any] =
    js.Array(that.name, that.id, that.startDate, that.endDate)
}



/*
// facade for the Timeline-Rows
object TimelineRow_Trait {
  def apply(
             id:        String,
             name:      String,
             startDate: js.Date,
             endDate:   js.Date,
             // Tooltips default values
             tooltips:  js.Object = new Tooltips()
           ) = js.Dynamic.literal(
    id = id,
    name = name,
    startDate = startDate,
    endDate = endDate,
    tooltips = tooltips
  )
}
*/