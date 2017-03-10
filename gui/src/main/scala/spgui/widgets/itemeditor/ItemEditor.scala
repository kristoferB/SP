package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import spgui._

object ItemEditor {

  private val component = ScalaComponent.build[SPWidgetBase]("ItemEditor")
    .render_P(p =>
    <.div(
      ^.className := ItemEditorCSS.editor.htmlClass,
      JSONEditorTest(p.getWidgetData, p.id)
    )
  )
    .build

  def apply() = SPWidget(spwb => component(spwb))
}
