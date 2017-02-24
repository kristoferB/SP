package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui._

object ItemEditor {

  private val component = ReactComponentB[SPWidgetBase]("ItemEditor")
    .render_P(p =>
    <.div(
      ^.className := ItemEditorCSS.editor.htmlClass,
      JSONEditorTest(p.getWidgetData, p.id)
    )
  )
    .build

  def apply() = (spwb: SPWidgetBase) => component(spwb)
}
