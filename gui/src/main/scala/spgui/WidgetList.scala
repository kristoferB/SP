package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spgui.widgets.examples._


object WidgetList {

  val list =
    List[(String, SPWidgetBase => ReactElement, Int, Int)](
      ("Grid Test", spgui.dashboard.GridTest(), 5, 5),
      ("Widget Injection", widgets.injection.WidgetInjectionTest(), 2, 2),
      ("Item Editor", widgets.itemeditor.ItemEditor(), 2, 2),
      ("DragDrop Example", widgets.examples.DragAndDrop(), 2, 2),
      ("Widget with json", widgets.examples.WidgetWithJSON(), 2, 2),
      ("PlcHldrC", PlaceholderComp(), 2, 2),
      ("SPWBTest", SPWidgetBaseTest(), 2, 2),
      ("Widget with data", widgets.examples.WidgetWithData(), 2, 2),
      ("CommTest", widgets.WidgetCommTest(), 2, 2),
      ("D3Example", widgets.examples.D3Example(), 2, 2),
      ("D3ExampleServiceWidget", widgets.examples.D3ExampleServiceWidget(), 2, 2),
      ("ExampleServiceWidget", ExampleServiceWidget(), 2, 2),
      ("ExampleServiceWidgetState", ExampleServiceWidgetState(), 2, 3),
      ("OpcUAWidget", OpcUAWidget(), 5, 2),
      ("ChartWithReact", widgets.examples.ChartWithReact(), 2, 2),
      ("Tree", widgets.itemexplorer.Tree(), 2, 4)
      
    )

  val map = list.map(t => t._1 -> (t._2, t._3, t._4)).toMap
}

object PlaceholderComp {
  val component = ReactComponentB[Unit]("PlaceholderComp")
    .render(_ => <.h2("placeholder"))
    .build

  def apply() = SPWidget(spwb => component())
}
