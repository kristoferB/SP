package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalajs.js
import org.scalajs.dom.raw.Element
import org.scalajs.dom.document

object JSONEditorTest {
  // TODO not that important but ideally we should get rid of the raw html ref
  private val jsoneditordivUglyCustomId = "jsoneditordivUglyCustomId"
  def apply() = component()
  private val component = ReactComponentB[Unit]("JSONEditorTest")
    .render(_ => jsoneditordiv)
    .componentDidMount(_ => Callback(addTheJSONEditor(jsoneditordivUglyCustomId)))
    .build
  private val jsoneditordiv = <.div(^.id := jsoneditordivUglyCustomId)

  // TODO type this stuff in some neat scalatastic manner
  val json = js.Dynamic.literal(
    "schema" -> js.Dynamic.literal(
      "type" -> "object",
      "properties" -> js.Dynamic.literal(
        "name" -> js.Dynamic.literal(
          "type" -> "string"
        )
      )
    )
  )

  private def addTheJSONEditor(elementId: String): Unit = {
    val editor = new JSONEditor(document.getElementById(elementId), json)
    editor.set(js.Dynamic.literal("name" -> "John Smith"))
  }
}

// TODO facade more stuff than just set
@js.native
class JSONEditor(element: Element, json: js.Object) extends js.Object {
  def set(json: js.Object): Unit = js.native
}
