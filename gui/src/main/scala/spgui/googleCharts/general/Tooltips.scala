/****************************************
  *      HELP CLASS TO GOOGLE CHARTS     *
  ****************************************/

package spgui.googleCharts.general

import scala.scalajs.js

/*
 * https://developers.google.com/chart/interactive/docs/customizing_tooltip_content
 * Tooltips are the little boxes that pop up when you hover over something
 */
trait TooltipsTrait {
  val isHtml: Boolean
  val trigger: String
}

class Tooltips (
                 override val isHtml: Boolean,
                 override val trigger: String
               ) extends TooltipsTrait {
  // auxilarry constructor with default values
  def this() = this(true, "focus")

  // method: toArray()
  // returns: an js.Array of js.Any
  def toArray(): js.Array[js.Any] =
    js.Array(this.isHtml, this.trigger)

  // toString method
  override def toString = s"Tooltips($isHtml, $trigger)"
}