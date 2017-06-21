/**
  * Created by alexa on 21/06/2017.
  */

package spgui.googleAPI.timeline

import scala.scalajs.js
import spgui.googleAPI.Tooltips


// TODO: Look through specs to see if this is really needed!

trait TimelineLogic_Trait {
  val list: List[TimelineRowTrait]
  val options: OptionsTimeline_Trait
}
class TimelineLogic (
                      override val list: List[TimelineRowTrait],
                      override val options: OptionsTimeline_Trait
                    )extends TimelineLogic_Trait {

}