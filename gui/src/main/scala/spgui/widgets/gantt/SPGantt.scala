package spgui.widgets.gantt

import org.scalajs.dom
import scalajs.js
import js.annotation.JSGlobal

@js.native
trait SPGantt extends js.Object { // facades "facadedObject in gui/src/main/resources/ganttApp.js
  def addSomeRow(): Unit = js.native
  def addRow(row: js.Object): Unit = js.native
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
}
// atm there is no perfect way to facade "options" kind of jsObjects, but this is one way
object Task {
  def apply(name: String, from: js.Date, to: js.Date): Task = {
    val jsObj = (new js.Object).asInstanceOf[Task]
    jsObj.name = name
    jsObj.from = from
    jsObj.to = to
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

