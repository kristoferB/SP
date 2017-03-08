package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

import sp.domain.SPValue

import scalajs.js
import scalajs.js.Dynamic.{ literal => l }
import org.scalajs.dom.raw

object JSONEditorTest {
  def component = ReactComponentB[SPValue]("JSONEditorTest")
    .render(_ => <.div(ItemEditorCSS.editor))
    .componentDidMount(dcb => Callback(addTheJSONEditor(dcb.props, dcb.getDOMNode)))
    .build

  def apply(data: SPValue) = component(data: SPValue)

  val schema = l(
    "title" -> "Example Schema",
    "type" -> "object",
    "properties" -> l(
      "firstName" -> l(
        "type" -> "string"
      ),
      "lastName" -> l(
        "type" -> "string"
      ),
      "gender" -> l(
        "enum" -> js.Array("male", "female")
      ),
      "age" -> l(
        "description" -> "Age in years",
        "type" -> "integer",
        "minimum" -> 0
      )
    ),
    "required" -> js.Array("firstName", "lastName")
  )

  val options = l(
    "mode" -> "tree",
    "modes" -> js.Array("code", "tree"),
    "schema" -> schema
  )

  val json = l(
    "firstName" -> "John",
    "lastName" -> "Doe",
    "gender" -> null,
    "age" -> 28
  )

  private def addTheJSONEditor(data: SPValue, element: raw.Element): Unit = {
    val editor = new JSONEditor(element, options, json)
  }
}

// TODO facade more stuff than just set
@js.native
class JSONEditor(element: raw.Element, options: js.UndefOr[js.Object] = js.undefined, json: js.UndefOr[js.Object] = js.undefined) extends js.Object {
  def set(json: js.Object): Unit = js.native
  def resize(): Unit = js.native
}
