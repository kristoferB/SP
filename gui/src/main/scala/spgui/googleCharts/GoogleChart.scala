/****************************************
  *      FACADE FOR GOOGLE CHARTS        *
  ****************************************/

package spgui.googleCharts

import scala.scalajs.js

@js.native
trait GoogleChart extends js.Object {
  // a div element where the chart should be drawn
  val element: js.Dynamic = js.native
  // Draws the chart
  def draw(data: DataTableAPI, options: js.Object): Unit = js.native
  // Clears the chart, and releases all of its allocated resources
  def clearChart(): Unit = js.native

  // TODO: Implement getSelection(): Array
  // Returns an array of the selected chart entities
  // def getSelection():
}

object GoogleChart {
  def apply(
           element: js.Dynamic
           ) = js.Dynamic.literal(
    element = element
  )
}