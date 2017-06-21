/**
  * Created by alexa on 21/06/2017.
  */
package spgui.googleAPI

import scala.scalajs.js

@js.native
trait GoogleChart_Trait extends js.Object {
  val element: js.Dynamic = js.native
  // Draws the chart
  def draw(data: DataTable_Trait, options: Options_Trait): Unit = js.native
  // Clears the chart, and releases all of its allocated resources
  def clearChart(): Unit = js.native

  // TODO: Implement getSelection(): Array
  // Returns an array of the selected chart entities
  // def getSelection():
}