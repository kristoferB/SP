package spgui.googleAPI.timeline

import scala.scalajs.js

/**
  * Created by alexa on 21/06/2017.
  */


// Configuration Options for Timeline

trait TimelineTrait {
  val barLabelStyle:     js.Object
  val colorByRowLabel:   Boolean
  val groupByRowLabel:   Boolean
  val rowLabelStyle:     js.Object
  val showBarLabels:     Boolean
  val showRowLabels:     Boolean
  val singleColor:       String
}

// null, false, true, null, true, true, null
class Timeline (
                        override val barLabelStyle: js.Object = null,
                        override val colorByRowLabel: Boolean = false,
                        override val groupByRowLabel: Boolean = true,
                        override val rowLabelStyle: js.Object = null,
                        override val showBarLabels: Boolean = true,
                        override val showRowLabels: Boolean = true,
                        override val singleColor: String = null
                      ) extends TimelineTrait {

}

/*
object OptionsTimelineInner {
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
}*/
