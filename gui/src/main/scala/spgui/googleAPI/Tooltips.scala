package spgui.googleAPI

import scala.scalajs.js

/**
  * Created by alexa on 21/06/2017.
  */

/*
 * https://developers.google.com/chart/interactive/docs/customizing_tooltip_content
 * Tooltips are the little boxes that pop up when you hover over something
 */
@js.native
trait Tooltips extends js.Object {
  val isHtml: Boolean = js.native
  val trigger: String = js.native
}

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
}