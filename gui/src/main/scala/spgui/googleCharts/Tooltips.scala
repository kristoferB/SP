package spgui.googleCharts

import scala.scalajs.js

/**
  * Created by alexa on 21/06/2017.
  */

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

  def this() = this(true, "focus")
  def toArray(): js.Array[js.Any] =
    js.Array(this.isHtml, this.trigger)


  override def toString = s"Tooltips($isHtml, $trigger)"
}

/*
// Facade for ToolTips
object Tooltips {
  def apply(
             isHtml: Boolean,
             trigger: String
           ) =
    js.Dynamic.literal(
      isHtml = isHtml,
      trigger = trigger
    )
}*/