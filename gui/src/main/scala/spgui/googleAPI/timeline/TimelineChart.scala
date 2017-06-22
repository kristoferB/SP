package spgui.googleAPI.timeline

import spgui.googleAPI.{DataTable_Trait, GoogleChart_Trait, Options_Trait}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@js.native
trait TimelineChart_Trait extends GoogleChart_Trait {
  override val element: js.Dynamic = js.native
}

@js.native
object TimelineChart extends js.Object

class TimelineChart(
                     override val element: js.Dynamic
                   ) extends TimelineChart_Trait {

}