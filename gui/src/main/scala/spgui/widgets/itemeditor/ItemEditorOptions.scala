package spgui.widgets.itemeditor

import scalajs.js
import js.Dynamic.{ literal => l }

object ItemEditorOptions {
  def apply() =
    JSONEditorOptions(
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
            // jsoneditor doesn't care, it seems, should facade onEditable
            "readOnly" -> true
          )
        ),
        "required" -> js.Array("isa", "name", "id")
      )
    )
}
