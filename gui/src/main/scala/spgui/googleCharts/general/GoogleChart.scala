/****************************************
  *      FACADE FOR GOOGLE CHARTS        *
  ****************************************/

package spgui.googleCharts.general

import scala.scalajs.js

@js.native
trait GoogleChartTrait extends js.Object {
  // a div element where the chart should be drawn
  val element: js.Dynamic = js.native
  // Draws the chart
  def draw(data: DataTableAPI, options: js.Object): Unit = js.native
  // Clears the chart, and releases all of its allocated resources
  def clearChart(): Unit = js.native


  // Returns an array of the selected chart entities
  def getSelection(): js.Array[js.Any] = js.native
}

object GoogleChart {
  def apply(
           element: js.Dynamic
           ) = js.Dynamic.literal(
    element = element
  )

}