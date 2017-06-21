package spgui.googleAPI.timeline

import scala.scalajs.js

/**
  * Created by alexa on 21/06/2017.
  */


// Configuration Options for Timeline
@js.native
trait OptionsTimelineInner_Trait extends js.Object {
  val barLabelStyle:     js.Object         = js.native
  val colorByRowLabel:   Boolean           = js.native
  val groupByRowLabel:   Boolean           = js.native
  val rowLabelStyle:     js.Object         = js.native
  val showBarLabels:     Boolean           = js.native
  val showRowLabels:     Boolean           = js.native
  val singleColor:       String            = js.native
}

// null, false, true, null, true, true, null
class OptionsTimelineInner (
                        override val barLabelStyle: js.Object = null,
                        override val colorByRowLabel: Boolean = false,
                        override val groupByRowLabel: Boolean = true,
                        override val rowLabelStyle: js.Object = null,
                        override val showBarLabels: Boolean = true,
                        override val showRowLabels: Boolean = true,
                        override val singleColor: String = null
                      ) extends OptionsTimelineInner_Trait {

}
/*
object OptionsTimelineInner_Trait {
  def apply(
             barLabelStyle:    js.Object,
             colorByRowLabel:  Boolean,
             groupByRowLabel:  Boolean,
             rowLabelStyle:    js.Object,
             showBarLabels:    Boolean,
             showRowLabels:    Boolean,
             singleColor:      String
           ) = js.Dynamic.literal(
    barLabelStyle = barLabelStyle,
    colorByRowLabel = colorByRowLabel,
    groupByRowLabel = groupByRowLabel,
    rowLabelStyle = rowLabelStyle,
    showBarLabels = showRowLabels,
    showRowLabels = showRowLabels,
    singleColor = singleColor)
}
*/