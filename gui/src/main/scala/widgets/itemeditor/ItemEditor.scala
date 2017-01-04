package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object ItemEditor {

  private val component = ReactComponentB[Unit]("ItemEditor")
    .render(_ =>
      <.div(
        <.h3("hello from ItemEditor"),
        JSONEditorTest()
      )
    )
    .build

  def apply(): ReactElement = component()
}
