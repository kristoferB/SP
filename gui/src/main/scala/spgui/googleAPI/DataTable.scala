/**
  * Created by alexa on 21/06/2017.
  */
package spgui.googleAPI

import scala.scalajs.js

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

object DataTable {

}
