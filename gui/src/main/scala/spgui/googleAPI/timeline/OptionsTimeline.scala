package spgui.googleAPI.timeline

import scala.scalajs.js

/**
  * Created by alexa on 21/06/2017.
  */


// Configuration Options for Timeline
// Changing this
@js.native
trait OptionsTimeline extends js.Object {
  val barLabelStyle:     js.Object         = js.native
  val colorByRowLabel:   Boolean           = js.native
  val groupByRowLabel:   Boolean           = js.native
  val rowLabelStyle:     js.Object         = js.native
  val showBarLabels:     Boolean           = js.native
  val showRowLabels:     Boolean           = js.native
  val singleColor:       String            = js.native
}

object OptionsTimeline {
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