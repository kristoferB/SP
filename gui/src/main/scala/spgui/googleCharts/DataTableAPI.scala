/**
  * Created by alexa on 21/06/2017.
  */
package spgui.googleCharts
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


  // add a new column to the DataTable,
  // type must be specified but label and id is optional
  def addColumn(`type`: String, opt_label: String, opt_id: String): Unit = js.native
  def addColumn(`type`: String): Unit = addColumn(`type`, "", "")


  // add a new column to the DataTable,
  // TODO: Fix passing of the two arguments to addColumn(..) above
  // argument:
  //          description_object  of type:
  //                                      DescriptionObject
  //  TODO: Fix result value to be current id, def addColumns(...): Number
  //
  def addColumn(description_object: Array[String]): Unit = js.native

  def addRow(row: js.Array[js.Any]): Unit = js.native
  // add an array of new rows to the DataTable
  // argument:
  //          row  of type:
  //                       GoogleRow
  //  TODO: Fix result value to be current id, def addRows(...): Number
  def addRows(rows: js.Array[js.Array[js.Any]]): Unit = js.native

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
}

object DataTableAPI {
  def apply(
           optional_data:     String = "",
           optional_version:  String = "0.6"
           ) = js.Dynamic.literal(
    optional_data = optional_data,
    optional_version = optional_version
  )
}

/*
@js.native
class DataTable (
                  override val optional_data: String = "",
                  override val optional_version: String = "0.6"
                ) extends DataTable_Trait {

}
*/