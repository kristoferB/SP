package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.widgets
import spgui.SPWidgetComp

object WidgetList {
  def apply() =
    Map[String, SPWidgetBase => ReactElement](
      ("Grid Test", spgui.dashboard.GridTest()),
      ("Widget Injection", widgets.injection.WidgetInjectionTest()),
      ("Item Editor", widgets.itemeditor.ItemEditor()),
      ("DragDrop Example", widgets.examples.DragAndDrop()),
      ("Widget with json", widgets.examples.WidgetWithJSON()),
      ("Ping", widgets.examples.Ping()),
      ("Pong", widgets.examples.Pong()),
      ("PlcHldrC", PlaceholderComp()),
      ("SPWBTest", SPWidgetBaseTest())
    )
}

object PlaceholderComp {
  val component = ReactComponentB[Unit]("PlaceholderComp")
    .render(_ => <.h2("placeholder"))
    .build

  def apply() = SPWidgetComp(spwb => component())
}
