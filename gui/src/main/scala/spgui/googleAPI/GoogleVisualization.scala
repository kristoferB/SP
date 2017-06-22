/**
  * Created by alexa on 15/06/2017.
  */

package spgui.googleAPI

import scala.scalajs.js

@js.native
trait GoogleVisualization extends js.Object {
  val charts:           List[GoogleChart_Trait] = js.native
  val dataTable:        DataTable_Trait         = js.native
}

object GoogleVisualization {
  def apply(
             charts: List[GoogleChart_Trait],
             dataTable: DataTable_Trait
           ) = js.Dynamic.literal(
    charts = charts,
    dataTable = dataTable
  )
}

/*
/**
  * Created by alexa on 15/06/2017.
  */

package spgui.googleAPI

import scala.scalajs.js

@js.native
object GoogleVisualization extends js.Object {
  @js.native
  class PieChart(element: js.Dynamic) extends js.Object {
    def draw(data: js.Any, options: js.Object): Unit = js.native
  }
  @js.native
  class Gantt(element: js.Dynamic) extends js.Object {
    def draw(data: js.Any, options: js.Object): Unit = js.native
  }
  @js.native
  class Timeline(element: js.Dynamic) extends js.Object {
    def draw(data: js.Any, options: js.Object): Unit = js.native
  }
  @js.native
  class DataTable extends js.Object {
    def addColumn(t: js.Any, d: js.Any): Unit = js.native
    //def addColumn(obj: js.Object): Unit = js.native
    def addRows(list: js.Any): Unit = js.native
  }
}
 */