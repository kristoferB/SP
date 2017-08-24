package spgui.googleCharts

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSName}

@js.native
@JSGlobal
object Charts extends js.Object {
  @JSName("google.charts.load")
  def load(): Unit = js.native
  @JSName("google.charts.load")
  def load(loading: String): Unit = js.native
  @JSName("google.charts.load")
  def load(loading: String, loadObject: js.Object): Unit = js.native

  @JSName("google.charts.setOnLoadCallback")
  def setOnLoadCallback(): Unit = js.native
  @JSName("google.charts.setOnLoadCallback")
  def setOnLoadCallback(callback: Unit): Unit = js.native
}

