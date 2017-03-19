package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._
import scalajs.js
import js.Dynamic.{ literal => l }

import spgui.SPWidgetBase
import sp.domain.SPValue
// TODO: function to convert SPValue to JSONEditor-props

object ItemEditor {

  private val jsonEditorOptions = JSONEditorOptions(
    mode = "tree",
    schema = l(
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
  )

  val json = l(
    "firstName" -> "John",
    "lastName" -> "Doe",
    "gender" -> null,
    "age" -> 28
  )

  private val component = ReactComponentB[SPWidgetBase]("ItemEditor")
    //.render_P(p => <.div(ItemEditorCSS.editor, JSONEditor(p.getWidgetData)))
    .render_P(p => <.div(ItemEditorCSS.editor, JSONEditor(jsonEditorOptions, json)))
    .build

  def apply() = (spwb: SPWidgetBase) => component(spwb)
}
