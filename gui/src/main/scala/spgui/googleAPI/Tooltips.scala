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
trait Tooltips_Trait extends js.Object {
  val isHtml: Boolean = js.native
  val trigger: String = js.native
}

@js.native
object Tooltips extends Tooltips_Trait

class Tooltips (
                 override val isHtml: Boolean = true,
                 override val trigger: String = "focus"
               ) extends Tooltips_Trait {

}

/*
// Facade for ToolTips
object Tooltips_Trait {
  def apply(
             isHtml: Boolean,
             trigger: String
           ) =
    js.Dynamic.literal(
      isHtml = isHtml,
      trigger = trigger
    )
}
*/