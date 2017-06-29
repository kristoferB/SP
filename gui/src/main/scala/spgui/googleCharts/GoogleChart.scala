/**
  * Created by alexa on 21/06/2017.
  */
package spgui.googleCharts

import scala.scalajs.js

@js.native
trait GoogleChart extends js.Object {
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