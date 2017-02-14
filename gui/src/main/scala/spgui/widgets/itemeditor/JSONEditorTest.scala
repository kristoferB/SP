package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalajs.js
import scalajs.js.Dynamic.{literal => l}
import org.scalajs.dom.raw.Element
import org.scalajs.dom.document
import java.util.concurrent.atomic.AtomicInteger
import spgui.circuit.{SPGUICircuit}

object JSONEditorTest {

  val incrementer = new AtomicInteger (0);
  def apply() = component("JSONEditorId-" + incrementer.incrementAndGet().toString)

  def component(id: String) = {
    ReactComponentB[Unit]("JSONEditorTest")
      .render_P(_ => <.div(
        ^.className := ItemEditorCSS.editor.htmlClass,
        ^.id := id
      )
    )
      .componentDidMount(_ => Callback({
        val editor = addTheJSONEditor(id)
        // set up the editor to explicitly resize when the layout is updated.
        // this fixes a bug introduced by placing jsoneditor inside an animated container
        SPGUICircuit.subscribe(SPGUICircuit.zoom(_.layout))(e => {
          editor.resize()
        })
      }))
      .build.apply()
  }

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
  private def addTheJSONEditor(elementId: String): JSONEditor = {
    val editor = new JSONEditor(document.getElementById(elementId), options)
    editor.set(l("name" -> "John Smith"))
    editor
  }
}

// TODO facade more stuff than just set
@js.native
class JSONEditor(element: Element, json: js.UndefOr[js.Object] = js.undefined) extends js.Object {
  def set(json: js.Object): Unit = js.native
  def resize(): Unit = js.native
}
