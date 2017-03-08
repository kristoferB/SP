package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

import spgui.SPWidgetBase

object ItemEditor {

  private val component = ReactComponentB[SPWidgetBase]("ItemEditor")
    .render_P(p => <.div(ItemEditorCSS.editor, JSONEditorTest(p.getWidgetData)))
    .build

  def apply() = (spwb: SPWidgetBase) => component(spwb)
}
