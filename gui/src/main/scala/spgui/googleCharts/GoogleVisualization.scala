 /****************************************
  *      FACADE FOR GOOGLE CHARTS        *
  ****************************************/
package spgui.googleCharts

import scala.scalajs.js


@js.native
object GoogleVisualization extends js.Object {
  // Example of Google Chart
  // Help methods for Timeline under googleCharts/timeline
  @js.native
  class Timeline(element: js.Dynamic) extends GoogleChart {
    override def draw(data: DataTableAPI, options: js.Object): Unit = js.native

    override def clearChart(): Unit = js.native
  }

  @js.native
  class DataTable(
                   optional_data:     String,
                   optional_version:  String
                 ) extends DataTableAPI {
    // Auxillary constructors
    def this(optionalData: String) = this(optionalData, "0.6")
    def this() = this("", "0.6")

    // add column from super-class
    override def addColumn(`type`: String, opt_label: String, opt_id: String): Unit =
      js.native

    override def addColumn(`type`: String): Unit =
      js.native

    override def addColumn(description_object: Array[String]): Unit =
      js.native


    // add rows from super-class
    override def addRows(rows: js.Array[js.Array[js.Any]]): Unit =
      js.native

    // add row from super-class
    override def addRow(row: js.Array[js.Any]): Unit =
      js.native

    // TODO - fix
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