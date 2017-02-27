package spgui.widgets.itemeditor

import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalajs.js
import scalajs.js.Dynamic.{literal => l}
import org.scalajs.dom.raw.Element
import org.scalajs.dom.document
import java.util.concurrent.atomic.AtomicInteger

import sp.domain.SPValue
import spgui.circuit.SPGUICircuit

object JSONEditorTest {

  val incrementer = new AtomicInteger (0);
  def apply(data: SPValue, id: UUID) = component(data: SPValue, id: UUID)

  def component(data: SPValue, id: UUID) = {
    ReactComponentB[Unit]("JSONEditorTest")
      .render_P(_ => <.div(
        ^.className := ItemEditorCSS.editor.htmlClass,
        ^.id := id.toString
      )
    )
      .componentDidMount(_ => Callback({
        val editor = addTheJSONEditor(data, id)
        // set up the editor to explicitly resize when the layout is updated.
        // this fixes a bug introduced by placing jsoneditor inside an animated container
        SPGUICircuit.subscribe(SPGUICircuit.zoom(_.openWidgets.xs(id).layout))(e => {
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
  private def addTheJSONEditor(data: SPValue, id: UUID): JSONEditor = {
    val editor = new JSONEditor(document.getElementById(id.toString), options)
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
