package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spgui.widgets.examples._


object WidgetList {
  val list =
    List[(String, SPWidgetBase => ReactElement)](
      ("Grid Test", spgui.dashboard.GridTest()),
      ("Widget Injection", widgets.injection.WidgetInjectionTest()),
      ("Item Editor", widgets.itemeditor.ItemEditor()),
      ("DragDrop Example", widgets.examples.DragAndDrop()),
      ("Widget with json", widgets.examples.WidgetWithJSON()),
      ("Ping", widgets.examples.Ping()),
      ("Pong", widgets.examples.Pong()),
      ("PlcHldrC", PlaceholderComp()),
      ("SPWBTest", SPWidgetBaseTest()),
      ("Widget with data", widgets.examples.WidgetWithData()),
      ("CommTest", widgets.WidgetCommTest()),
      ("D3Test", widgets.examples.D3Example()),
      ("ExampleServiceWidget", ExampleServiceWidget()),
      ("OpcUAWidget", OpcUAWidget())
    )

  val map = list.toMap
}

object PlaceholderComp {
  val component = ReactComponentB[Unit]("PlaceholderComp")
    .render(_ => <.h2("placeholder"))
    .build

  def apply() = SPWidget(spwb => component())
}
