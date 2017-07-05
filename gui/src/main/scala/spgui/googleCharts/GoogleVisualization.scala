 /****************************************
  *      FACADE FOR GOOGLE CHARTS        *
  ****************************************/
package spgui.googleCharts

import spgui.googleCharts.general.{DataTableAPI, GoogleChartTrait}

import scala.scalajs.js


@js.native
object GoogleVisualization extends js.Object {
  // Example of Google Chart
  // Help methods for Timeline under googleCharts/timeline
  @js.native
  class Timeline(element: js.Dynamic) extends GoogleChartTrait {
    // draw()-function in API
    // Arguments:
    // -data:     DataTableAPI
    // -options:  js.Object
    // no result value
    override def draw(data: DataTableAPI, options: js.Object): Unit = js.native
    // clearChart()-function in API
    // Arguments: -none
    // no result value
    override def clearChart(): Unit = js.native
  }

  @js.native
  class DataTable(
                   optional_data:     String,
                   optional_version:  String
                 ) extends DataTableAPI {
     /********************************
      *    AUXILARRY CONSTRUCTORS    *
      * ******************************/
    def this(optionalData: String) = this(optionalData, "0.6")
    def this() = this("", "0.6")



     /********************************
      *         COLUMNS              *
      * ******************************/
    // add column with type, label and id
    override def addColumn(`type`: String, opt_label: String, opt_id: String): Unit =
      js.native
    // add column with type
    override def addColumn(`type`: String): Unit =
      js.native
    // add column with a description_object
    override def addColumn(description_object: Array[String]): Unit =
      js.native


     /********************************
      *             ROWS             *
      * ******************************/

    // addRows()-method from API
    // add several rows to dataTable at the same time
    // Arguments:
    // -rows: Array of row (which is a array of Any)
    // no result value
    override def addRows(rows: js.Array[js.Array[js.Any]]): Unit =
      js.native

    // addRow()-method from API
    // add several rows to dataTable at the same time
    // Arguments:
    // -row: a array of Any
    // no result value
    override def addRow(row: js.Array[js.Any]): Unit =
      js.native

    // addRow()-method from API
    // add an empty row to dataTable
    override def addRow(): Unit = js.native
    // returns the numbers of rows
    override def getNumberOfRows(): Int = js.native



    // Returns a clone of the data table.
    override def clone(): DataTableAPI = js.native

    // See 'Format of the Constructor's JavaScript Literal data Parameter' under methods
    // https://developers.google.com/chart/interactive/docs/reference#methods
    def toJSON(): String = js.native

  }
}

/*
OLD CODE - Sometimes a helping hand

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