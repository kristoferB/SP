/**
  * Created by alexa on 15/06/2017.
  */

package spgui.googleAPI

import scala.scalajs.js

trait DescriptionObject extends js.Object {
  val column_type: String = js.native
  val optional_label: String = js.native
  val optional_id: String = js.native
  val optional_role: String
}

// Documentation DataTable
// Constructor : https://developers.google.com/chart/interactive/docs/reference#datatable-class
@js.native
trait DataTable extends js.Object {
  // ex. get data from DataTable.toJSON()
  val optional_data:    String = js.native
  // 2017-06-21
  // CURRENT VERSION: 0.6
  val optional_version: String = js.native

  // add a new column to the DataTable
  def addColumn(col_type: String, optional_label: String, optional_id: String): Unit = js.native
  def addRows(): Unit = js.native
}


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

