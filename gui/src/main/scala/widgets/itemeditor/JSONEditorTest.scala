package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalajs.js
import scalajs.js.Dynamic.{literal => l}
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
  private val jsoneditordiv = <.div(
    ^.id := jsoneditordivUglyCustomId
  )

  // TODO type this stuff in some neat scalatastic manner
  val json = l(
      "type" -> "object",
      "properties" -> l(
        "name" -> l(
          "type" -> "string"
      )
    )
  )

  val options = l(
    "mode" -> "code",
    "modes" -> js.Array("code", "tree")
  )

  private def addTheJSONEditor(elementId: String): Unit = {
    val editor = new JSONEditor(document.getElementById(elementId), options)
    editor.set(l("name" -> "John Smith"))
  }
}

// TODO facade more stuff than just set
@js.native
class JSONEditor(element: Element, json: js.UndefOr[js.Object] = js.undefined) extends js.Object {
  def set(json: js.Object): Unit = js.native
}
