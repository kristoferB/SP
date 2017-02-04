package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.widgets
import spgui.SPWidget

object WidgetList {
  def apply() =
    Map[String, SPWidget](
      ("Grid Test", spgui.dashboard.GridTest),
      ("Widget Injection", widgets.injection.WidgetInjectionTest),
      ("Item Editor", widgets.itemeditor.ItemEditor),
      ("DragDrop Example", widgets.examples.DragAndDrop),
      ("Widget with json", widgets.examples.WidgetWithJSON),
      ("PlcHldrC", PlaceholderComp)
    )
}

object PlaceholderComp extends SPWidget {
  val component = ReactComponentB[Unit]("PlaceholderComp")
    .render(_ => <.h2("placeholder"))
    .build

  def renderWidget = <.div("hej")

  override def apply(id: Int) = component()
}
