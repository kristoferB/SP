package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.widgets

object WidgetList {
  def apply() =
    Map[String, ReactElement](
      ("Grid Test", spgui.dashboard.Grid.component()),
      ("Widget Injection", widgets.injection.WidgetInjectionTest()),
      ("Item Editor", widgets.itemeditor.ItemEditor()),
      ("DragDrop Example", widgets.examples.dragndrop.DragAndDrop()),
      ("PlcHldrC", PlaceholderComp())
    )
}

object PlaceholderComp {
  val component = ReactComponentB[Unit]("PlaceholderComp")
    .render(_ => <.h2("placeholder"))
    .build

  def apply() = component()
}
