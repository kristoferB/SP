/****************************************
  *      HELP CLASS TO GOOGLE CHARTS     *
  ****************************************/

package spgui.googleCharts.general

import scala.scalajs.js


// this is a general options trait for the google charts
// extend the trait for each new chart implemented
trait OptionsTrait {
  var height: Int
  var width: Int

  def toDynamic(): js.Object
}