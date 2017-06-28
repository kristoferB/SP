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
  class Timeline(element: js.Dynamic) extends GoogleChart {
    override def draw(data: DataTableAPI, options: js.Object): Unit = js.native
    def draw(data: DataTableAPI): Unit = draw(data, new js.Object())
  }

  @js.native
  class DataTable extends DataTableAPI {
    // add column from super-class
    override def addColumn(`type`: String, opt_label: String, opt_id: String): Unit =
      super.addColumn(`type`, opt_label, opt_id)

    override def addColumn(`type`: String): Unit =
      super.addColumn(`type`)

    override def addColumn(description_object: Array[String]): Unit =
      super.addColumn(description_object)



    // add row from super-class
    override def addRows(rows: js.Array[js.Array[js.Any]]): Unit =
      super.addRows(rows)

    override def addRow(row: js.Array[js.Any]): Unit =
      super.addRow(row)
  }
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