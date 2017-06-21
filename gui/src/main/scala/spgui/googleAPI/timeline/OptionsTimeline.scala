/**
  * Created by alexa on 15/06/2017.
  */
package spgui.googleAPI.timeline

import scala.scalajs.js
import spgui.googleAPI.{Options_Trait, Tooltips, Tooltips_Trait}

/*
 * Full Documentation Google Charts
 * https://developers.google.com/chart/interactive/docs/reference#top_of_page
 */

/*
 * Timeline Doucmentation:
 * https://developers.google.com/chart/interactive/docs/gallery/timeline#configuration-options
 */

// See Timeline Docs
@js.native
trait OptionsTimeline_Trait extends Options_Trait {
  val avoidOverlappingGridLines:  Boolean           = js.native
  val backgroundColor:            String            = js.native
  val colors:                     js.Array[String]  = js.native
  val enableInteractivity:        Boolean           = js.native
  val fontName:                   String            = js.native
  val fontSize:                   String            = js.native
  val forceIFrame:                Boolean           = js.native
  override val height:            Int               = js.native
  val optionsTimelineInner:       OptionsTimelineInner_Trait = js.native
  val tooltips:                   Tooltips_Trait    = js.native
  override val width:             Int               = js.native
}



class OptionsTimeline (
                override val height: Int,
                override val width: Int,
                override val avoidOverlappingGridLines: Boolean = true,
                override val backgroundColor: String = "white",
                override val colors: js.Array[String] = null,
                override val enableInteractivity: Boolean = true,
                override val fontName: String = "Arial",
                override val fontSize: String = "automatic",
                override val forceIFrame: Boolean = false,
                override val optionsTimelineInner: OptionsTimelineInner_Trait = new OptionsTimelineInner,
                override val tooltips: Tooltips_Trait = new Tooltips
              )extends OptionsTimeline_Trait {

}



/*
// Facade for the Options
object OptionsTimeline_Trait {

  def apply(
             // height and width must be set
             height:                      Int,
             width:                       Int,
             // see full doc for default values
             // name:                     Type      = Default value

             avoidOverlappingGridLines:   Boolean   = true,
             backgroundColor:             String    = "white",
             colors:                      js.Array[String] = null,
             enableInteractivity:         Boolean   = true,
             fontName:                    String    = "Arial",
             fontSize:                    String = "automatic",
             forceIFrame:                 Boolean = false,
             optionsTimeline:             js.Object = OptionsTimeline(
               null, false, true, null, true, true, null
             ),
             tooltips:                    js.Object = Tooltips(true, "focus")
           ) = js.Dynamic.literal(
    avoidOverlappingGridLines = avoidOverlappingGridLines,
    backgroundColor = backgroundColor,
    colors = colors,
    enableInteractivity = enableInteractivity,
    fontName = fontName,
    fontSize = fontSize,
    forceIFrame = forceIFrame,
    height = height,
    optionsTimeline = optionsTimeline,
    tooltips = tooltips,
    width = width
  )
}
*/