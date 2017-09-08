package spgui.widgets.gantt

import org.scalajs.dom
import scalajs.js
import js.annotation.JSGlobal

@js.native
trait SPGantt extends js.Object { // facades "facadedObject" in gui/src/main/resources/ganttApp.js
  def setData(rows: js.Array[Row]): Unit = js.native
  def addSomeRow(): Unit = js.native
  def addRow(row: Row): Unit = js.native
}
@js.native
@JSGlobal("SPGantt")
object SPGantt extends js.Object {
  def apply(element: dom.Element): SPGantt = js.native
}

@js.native
trait Task extends js.Object {
  var name: String = js.native
  var from: js.Date = js.native
  var to: js.Date = js.native
  var color: String = js.native // actually js.UndefOr but setting to null gives correct behaviour
}
// atm there is no perfect way to facade "options" kind of jsObjects, but this is one way
object Task {
  def apply(name: String, from: js.Date, to: js.Date, color: String = null): Task = {
    val jsObj = (new js.Object).asInstanceOf[Task]
    jsObj.name = name
    jsObj.from = from
    jsObj.to = to
    jsObj.color = color
    jsObj
  }
}

@js.native
trait Row extends js.Object {
  var name: String = js.native
  var tasks: js.Array[Task] = js.native
}
object Row {
  def apply(name: String, tasks: js.Array[Task]): Row = {
    val jsObj = (new js.Object).asInstanceOf[Row]
    jsObj.name = name
    jsObj.tasks = tasks
    jsObj
  }
}

