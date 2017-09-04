package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import spgui.widgets.examples._


object WidgetList {
  val list =
    List[(String, SPWidgetBase => VdomElement, Int, Int)](
      ("Grid Test",                   spgui.dashboard.GridTest(),                    5, 5),
      ("Widget Injection",            widgets.injection.WidgetInjectionTest(),       3, 4),
      ("Item Editor",                 widgets.itemeditor.ItemEditor(),               3, 4),
      ("DragDrop Example",            widgets.examples.DragAndDrop(),                3, 4),
      ("Widget with json",            widgets.examples.WidgetWithJSON(),             3, 4),
      ("PlcHldrC",                    PlaceholderComp(),                             3, 4),
      ("SPWBTest",                    SPWidgetBaseTest(),                            3, 4),
      ("Widget with data",            widgets.examples.WidgetWithData(),             3, 4),
      ("CommTest",                    widgets.WidgetCommTest(),                      3, 4),
      ("D3Example",                   widgets.examples.D3Example(),                  3, 4),
      ("D3ExampleServiceWidget",      widgets.examples.D3ExampleServiceWidget(),     3, 4),
      ("GanttExample",                widgets.gantt.GanttExample(),                  10, 6),
      ("ExampleServiceWidget",        ExampleServiceWidget(),                        3, 4),
      ("ExampleServiceWidgetState",   ExampleServiceWidgetState(),                   3, 3),
      ("OpcUAWidget",                 OpcUAWidget(),                                 5, 4),
      ("Item explorer",               widgets.itemexplorer.ItemExplorer(),           3, 4),
      ("Ability Handler",             widgets.abilityhandler.AbilityHandlerWidget(), 3, 4),
      ("ServiceList",                 widgets.services.ServiceListWidget(),          3, 4),
      ("ComponentTest",               widgets.componenttest.ComponentTest(),         3, 4),      
      ("SopMaker",                    widgets.sopmaker.SopMakerWidget(),             3, 4)
    )

  val map = list.map(t => t._1 -> (t._2, t._3, t._4)).toMap
}

object PlaceholderComp {
  val component = ScalaComponent.builder[Unit]("PlaceholderComp")
    .render(_ => <.h2("placeholder"))
    .build

  def apply() = SPWidget(spwb => component())
}
