package spgui.googleAPI.timeline

import spgui.googleAPI.{DataTable_Trait, GoogleChart_Trait, Options_Trait}

import scala.scalajs.js

@js.native
trait TimelineChart_Trait extends GoogleChart_Trait {
  override val element: js.Dynamic = js.native
}

@js.native
class TimelineChart(
                     override val element: js.Dynamic
                   ) extends TimelineChart_Trait {

}