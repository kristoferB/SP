/**
  * Created by alexa on 21/06/2017.
  */
package spgui.googleAPI

import scala.scalajs.js

// Documentation DataTable
// Constructor : https://developers.google.com/chart/interactive/docs/reference#datatable-class

@js.native
trait DataTable_Trait extends js.Object {
  // ex. get data from DataTable.toJSON()
  val optional_data:    String = js.native

  // 2017-06-21
  // CURRENT VERSION: 0.6
  val optional_version: String = js.native

  // add a new column to the DataTable
  // argument:
  //          description_object  of type:
  //                                      DescriptionObject
  //  TODO: Fix result value to be current id, def addColumns(...): Number
  //
  def addColumn(desc_obj: DescriptionObject_Trait): Unit = js.native
  // add a new row to the DataTable
  // argument:
  //          row  of type:
  //                       GoogleRow
  //  TODO: Fix result value to be current id, def addRows(...): Number
  def addRows(row: GoogleRow_Trait): Unit = js.native

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

class DataTable (
                  override val optional_data: String = "",
                  override val optional_version: String = "0.6"
                ) extends DataTable_Trait {
  override def addColumn(desc_obj: DescriptionObject_Trait): Unit = super.addColumn(desc_obj)
  override def addRows(row: GoogleRow_Trait): Unit = super.addRows(row)
}
