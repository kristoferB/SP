/**
  * Created by alexa on 21/06/2017.
  */
package spgui.googleAPI.timeline

import spgui.googleAPI.{GoogleRow_Trait, Tooltips}

import scala.scalajs.js
import scala.scalajs.js.Date

// Doc. Data format
// https://developers.google.com/chart/interactive/docs/gallery/timeline#data-format
@js.native
trait TimelineRow_Trait_Trait extends GoogleRow_Trait {
  override val id: String = js.native
  override val name: String = js.native
  override val tooltips: Tooltips = js.native
  override val startDate: Date = js.native
  override val endDate: Date = js.native
}

class TimelineRowTrait(
                    override val id: String,
                    override val name: String,
                    override val startDate: Date,
                    override val endDate: Date,
                    override val tooltips: Tooltips = new Tooltips()
                  ) extends TimelineRow_Trait_Trait {

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