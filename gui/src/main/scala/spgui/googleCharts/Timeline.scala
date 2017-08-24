package spgui.googleCharts

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal


@js.native
@JSGlobal("google.visualization.Timeline")
class Timeline(container: js.Dynamic) extends js.Object {
  def draw(data: DataTable): Unit = js.native
  def draw(data: DataView): Unit = js.native
  def draw(data: DataTable, options: js.Object): Unit = js.native
  def draw(data: DataView, options: js.Object): Unit = js.native

  def clearChart(): Unit = js.native

  def getSelection(): js.Array[js.Any] = js.native
}