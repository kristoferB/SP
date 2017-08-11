package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
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
      "title" -> "SP item",
      "type" -> "object",
      "properties" -> l(
        "isa" -> l(
          "enum" -> js.Array("HierarchyRoot", "Operation", "SOPSpec")
        ),
        "name" -> l(
          "type" -> "string"
        ),
        /*
        "age" -> l(
          "description" -> "Age in years",
          "type" -> "integer",
          "minimum" -> 0
        ),
         */
        "id" -> l(
          // dk if this is visible somewhere, but I think it would be nice
          "description" -> "UUID as string",
          "type" -> "string",
          // jsoneditor doesn't, care, it seems, should facade onEditable
          "readOnly" -> true
        )
      ),
      "required" -> js.Array("isa", "name", "id")
    )
  )

  val json = l(
    "isa" -> "SomethingIncorrect",
    "name" -> "SampleJSON",
    "id" -> "this-should-be-an-uuid"
  )

  private val component = ScalaComponent.builder[SPWidgetBase]("ItemEditor")
    //.render_P(p => <.div(ItemEditorCSS.editor, JSONEditor(p.getWidgetData)))
    .render_P(p => <.div(ItemEditorCSS.editor, JSONEditor(jsonEditorOptions, json)))
    .build

  def apply() = (spwb: SPWidgetBase) => component(spwb)
}
