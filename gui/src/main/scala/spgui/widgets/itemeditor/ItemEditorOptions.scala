package spgui.widgets.itemeditor

import scalajs.js
import js.Dynamic.{ literal => l }

object ItemEditorOptions {
  def apply() =
    JSONEditorOptions(
      mode = "code",
      schema = itemEditorSchema
    )

  def onItemNodeEditable(node: js.Dynamic) = {
    // js-object in tree mode, boolean in code mode, see jsonEditor API
    var editable: Any = null

    // node is part of jsonEditor API and has members field, value and path
    val field = node.selectDynamic("field")
    // val value = node.selectDynamic("value") // works
    // val path = node.selectDynamic("path") // prints nothing

    if(field == js.undefined) { // this means we are in code mode, in our case, see API
      editable = false
    } else {
      // field names never editable, values editable except the one for id
      val fieldNameEditable = false
      val valueEditable = if(field == "id") false else true // compiler warns, but it works
      editable = l("field" -> fieldNameEditable, "value" -> valueEditable)
    }

    editable
  }

  val itemEditorSchema = l(
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
        "type" -> "string"
        // // jsoneditor doesn't care, it seems, should facade onEditable
        // "readOnly" -> true
      )
    ),
    "required" -> js.Array("isa", "name", "id")
  )

}
