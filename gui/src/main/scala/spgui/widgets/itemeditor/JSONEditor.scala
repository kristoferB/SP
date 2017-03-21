package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

import sp.domain.SPValue

import scalajs.js
import js.Dynamic.{ literal => l }
import js.JSConverters._
import org.scalajs.dom.raw

// the state of the jsoneditor is best handled by the jsoneditor-module itself
// the whole jsoneditor-element is put as state to let it do that
object JSONEditor {
  case class Props(options: JSONEditorOptions, json: js.Dynamic)

  val component = ReactComponentB[Props]("JSONEditor")
    .initialState_P(p => JSONEditorElement(p.options, p.json))
    .render(dcb => dcb.state)
    .build

  def apply(options: JSONEditorOptions, json: js.Dynamic) = component(Props(options, json))
}

object JSONEditorElement {
  case class Props(options: JSONEditorOptions, json: js.Dynamic)

  def component = ReactComponentB[Props]("JSONEditorElement")
    .render(_ => <.div(ItemEditorCSS.editor))
    .componentDidMount(
      dcb => Callback(addTheJSONEditor(dcb.getDOMNode, dcb.props.options, dcb.props.json))
    )
    .build

  def apply(options: JSONEditorOptions, json: js.Dynamic) = component(Props(options, json))

  private def addTheJSONEditor(element: raw.Element, options: JSONEditorOptions, json: js.Dynamic): Unit = {
    val optionsInJS = options.toJS
    val editor = new JSONEditor(element, optionsInJS, json)
  }
}

// TODO facade more stuff than just set
// TODO facading destroy() perhaps a good idea
@js.native
class JSONEditor(element: raw.Element, options: js.UndefOr[js.Dynamic] = js.undefined, json: js.UndefOr[js.Dynamic] = js.undefined) extends js.Object {
  def set(json: js.Object): Unit = js.native
  def resize(): Unit = js.native
}

// this is actually a facade, even tho no annotation is needed
// TODO facade more of the options object
case class JSONEditorOptions(
  history: Boolean = true,
  mode: String = "code",
  modes: Seq[String] = Seq("code", "tree"),
  schema: js.UndefOr[js.Object] = js.undefined,
  search: Boolean = true
) {
  def toJS =
    l(
      "history" -> history,
      "mode" -> mode,
      "modes" -> modes.toJSArray,
      "schema" -> schema,
      "search" -> search
    )
}
