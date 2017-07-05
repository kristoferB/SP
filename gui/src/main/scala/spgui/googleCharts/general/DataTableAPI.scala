/****************************************
  *      FACADE FOR GOOGLE CHARTS        *
  ****************************************/

package spgui.googleCharts.general

import scala.scalajs.js
// Documentation DataTable
// Constructor : https://developers.google.com/chart/interactive/docs/reference#datatable-class

@js.native
trait DataTableAPI extends js.Object {
  // ex. get data from DataTable.toJSON()
  val optional_data:    String = js.native

  // 2017-06-21
  // CURRENT VERSION: 0.6
  val optional_version: String = js.native

   /********************************
    *         COLUMNS              *
    * ******************************/

  // add a new column to the DataTable,
  // type must be specified but label and id is optional
  def addColumn(`type`: String, opt_label: String, opt_id: String): Unit = js.native
  def addColumn(`type`: String): Unit = js.native
  // add a new column to the DataTable,
  // argument:
  //          description_object  of type:
  //                                      Array of Strings
  //  TODO: Fix result value to be current id, def addColumns(...): Number
  //
  def addColumn(description_object: Array[String]): Unit = js.native
  // Returns the number of columns in the table
  def getNumberOfColumns(): Int = js.native
  // Returns the type of a given column specified by the column index
  def getColumnType(columnIndex: Int): String = js.native



   /********************************
    *            ROWS              *
    * ******************************/
  // add a new row to DataTable
  // argument: row of type - js.Array of js.Any
  // Todo: fix result value
  def addRow(row: js.Array[js.Any]): Unit = js.native
  def addRow(): Unit = js.native
  // add an array of new rows to the DataTable
  // argument:
  //          rows  of type:
  //                       js.Array of js.Array of js.Any
  //  TODO: Fix result value to be current id, def addRows(...): Number
  def addRows(rows: js.Array[js.Array[js.Any]]): Unit = js.native

  // filters the rows, see help-class googleCharts.general.Filters
  def getFilteredRows(filters: js.Array[js.Object]): js.Array[js.Object] = js.native

  // get number of rows in datatable
  // example use in options to set height
  def getNumberOfRows(): Int = js.native

  override def clone(): DataTableAPI = js.native
  /*
   * TODO: Add more of the methods from google-charts API
   *
   * Ex:
   * add getColumnRange(columnIndex)
   * Descr. : Returns the minimal and maximal values
   *          of values in a specified column.
   *          The returned object has properties min and max.
   *          If the range has no values, min and max will contain null.
   *
   * private var index_column: Int = js.native
   * private var index_row: Int = js.native
   */

  // Returns a map of all properties for the table
  def getTableProperties(): js.Object = js.native
}

object DataTableAPI {
  def apply(
           optional_data:     String,
           optional_version:  String
           ) = js.Dynamic.literal(
    optional_data = optional_data,
    optional_version = optional_version
  )
}