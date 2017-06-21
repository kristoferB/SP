/**
  * Created by alexa on 21/06/2017.
  */
package spgui.googleAPI

import scala.scalajs.js
import js.annotation.JSExport

// Generalised Row for charts
// TODO: Add extensions for ex Google Gant

@JSExport
trait GoogleRow_Trait extends js.Object {
  val id:         String          = js.native
  val name:       String          = js.native
  val tooltips:   Tooltips        = js.native
  val startDate:  js.Date         = js.native
  val endDate:    js.Date         = js.native
}
