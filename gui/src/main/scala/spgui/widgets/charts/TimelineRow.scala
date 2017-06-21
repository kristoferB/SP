package spgui.widgets.charts

import scala.scalajs.js

/**
  * Created by alexa on 21/06/2017.
  */

// Doc. Data format
// https://developers.google.com/chart/interactive/docs/gallery/timeline#data-format
@js.native
trait TimelineRow extends js.Object {
  val id:         String          = js.native
  val name:       String          = js.native
  val tooltips:   Tooltips        = js.native
  val startDate:  js.Date         = js.native
  val endDate:    js.Date         = js.native
}

// facade for the Timeline-Rows
object TimelineRow {
  def apply(
             id:        String,
             name:      String,
             startDate: js.Date,
             endDate:   js.Date,
             tooltips:  js.Object = Tooltips(true, "focus")
           ) = js.Dynamic.literal(
    id = id,
    name = name,
    startDate = startDate,
    endDate = endDate,
    tooltips = tooltips
  )
}