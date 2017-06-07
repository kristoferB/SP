package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

import sp.domain.SPValue

import scalajs.js
import js.Dynamic.{ literal => l }
import js.JSConverters._
import org.scalajs.dom.raw

object JSONEditor {
  def apply(element: raw.Element, options: JSONEditorOptions): JSONEditor = {
    val optionsInJS = options.toJS
    new JSONEditor(element, optionsInJS)
  }
}

// TODO facade more stuff
// TODO facading destroy() perhaps a good idea
@js.native
class JSONEditor(
  element: raw.Element,
  options: js.UndefOr[js.Dynamic] = js.undefined,
  json: js.UndefOr[js.Dynamic] = js.undefined
) extends js.Object {
  def set(json: js.Dynamic): Unit = js.native
  def resize(): Unit = js.native
  def get(): js.Dynamic = js.native
}

// this is actually a facade, even tho no annotation is needed
// TODO facade more of the options object
case class JSONEditorOptions(
  // return boolean or object e.g. {field: true, value: false}
  onEditable: js.Dynamic => Any = node => false,
  history: Boolean = true,
  mode: String = "code",
  modes: Seq[String] = Seq("code", "tree"),
  schema: js.UndefOr[js.Dynamic] = js.undefined,
  search: Boolean = true
) {
  def toJS =
    l(
      "onEditable" -> onEditable,
      "history" -> history,
      "mode" -> mode,
      "modes" -> modes.toJSArray,
      "schema" -> schema,
      "search" -> search
    )
}
