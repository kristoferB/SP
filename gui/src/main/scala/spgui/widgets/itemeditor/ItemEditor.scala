package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._
import scalajs.js
import js.Dynamic.{ literal => l }
import js.JSON

import spgui.SPWidgetBase
import sp.domain._

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
          // jsoneditor doesn't care, it seems, should facade onEditable
          "readOnly" -> true
        )
      ),
      "required" -> js.Array("isa", "name", "id")
    )
  )

  val sampleJson = l(
    "isa" -> "SomethingIncorrect",
    "name" -> "SampleJSON",
    "id" -> "this-should-be-an-uuid"
  )

  class Backend($: BackendScope[SPWidgetBase, Unit]) {
    def getDataOrSample(spV: SPValue) = if(spV == SPValue.empty) sampleJson else JSON.parse(spV.toJson)

    def render(spwb: SPWidgetBase) = <.div(
      JSONEditor(options = jsonEditorOptions, json = getDataOrSample(spwb.getWidgetData)),
      ItemEditorCSS.editor
    )
  }

  private val component = ReactComponentB[SPWidgetBase]("ItemEditor")
    .renderBackend[Backend]
    .build

  def apply() = (spwb: SPWidgetBase) => component(spwb)
}

/*
// how to use a case class, convert it to SPValue, turn it to actual json, and how to get it back to case class
import sp.domain._
import sp.messages.Pickles.fromJsonToSPValue

case class SomeCC(isa: String, id: String)
val someCC = SomeCC(isa = "något", id = "någotAnnat")

val spValue = SPValue(someCC)
println(spValue) // pretty prints spv as json

val spValueAsJSDynamic = JSON.parse(spValue.toJson)
println(spValueAsJSDynamic) // prints [object Object]

val retrievedSPVal = fromJsonToSPValue(JSON.stringify(spValueAsJSDynamic)).get // fromJson.. returns a Try
  println(retrievedSPVal) // pretty prints spv as json

val retrievedCC = retrievedSPVal.getAs[SomeCC]("").get // getAs returns an Option
println(retrievedCC.id) // prints "någotAnnat"
 */

