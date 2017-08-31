package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._


object WidgetList {

   val erica =
    List[(String, SPWidgetBase => VdomElement, Int, Int)](
      ("Klocka", widgets.ClockWidget(), 2, 2),

      // Team widgets -->
      ("Triagediagram", widgets.TriageWidget(), 2, 8),
      ("Statusdiagram", widgets.StatusWidget(), 2, 3),
      ("Platsdiagram", widgets.PlaceWidget(), 2, 3),
      ("Patientkort", widgets.PatientCardsWidget(), 5, 19),
      ("Felsökning", widgets.DebuggingWidget(), 5, 19),
      ("Lång tid sedan händelse", widgets.PatientReminderWidget(), 1, 19),
      // <--

      // Gantt
      ("Patienter inne", widgets.charts.PatientGanttWidget() , 2, 2),

      // Coordinator widgets -->
      ("Rumskarta (koordinator)", widgets.RoomOverviewWidget(), 4, 15),
      ("Triage- och statusdiagram (koordinator)", widgets.CoordinatorDiagramWidget(), 3, 17),
      ("Väntrumsdiagram (koordinator)", widgets.WaitingRoomWidget(), 3, 4)
      // <--
    )


  val sp =
    List[(String, SPWidgetBase => VdomElement, Int, Int)](
      ("Grid Test",                   spgui.dashboard.GridTest(),                    5, 5),
      ("Widget Injection",            widgets.injection.WidgetInjectionTest(),       3, 4),
      ("Item Editor",                 widgets.itemeditor.ItemEditor(),               3, 4),
      ("DragDrop Example",            widgets.examples.DragAndDrop(),                3, 4),
      ("Widget with json",            widgets.examples.WidgetWithJSON(),             3, 4),
      ("PlcHldrC",                    PlaceholderComp(),                             3, 4),
      ("SPWBTest",                    SPWidgetBaseTest(),                            3, 4),
      ("Widget with data",            widgets.examples.WidgetWithData(),             3, 4),
      ("D3Example",                   widgets.examples.D3Example(),                  3, 4),
      ("D3ExampleServiceWidget",      widgets.examples.D3ExampleServiceWidget(),     3, 4),
      ("ExampleServiceWidget",        widgets.examples.ExampleServiceWidget(),                        3, 4),
      ("ExampleServiceWidgetState",   widgets.examples.ExampleServiceWidgetState(),                   3, 3),
      ("OpcUAWidget",                 widgets.examples.OpcUAWidget(),                                 5, 4),
      ("Item explorer",               widgets.itemexplorer.ItemExplorer(),           3, 4),
	    ("Item explorer tree", widgets.itemtree.ItemExplorer(), 2, 4),
      ("Ability Handler",             widgets.abilityhandler.AbilityHandlerWidget(), 3, 4),
      ("ServiceList",                 widgets.services.ServiceListWidget(),          3, 4),
      ("ComponentTest",               widgets.componenttest.ComponentTest(),         3, 4),      
      ("SopMaker",                    widgets.sopmaker.SopMakerWidget(),             3, 4)
    )

   val list = erica ++ sp

  val map = list.map(t => t._1 -> (t._2, t._3, t._4)).toMap
}

object PlaceholderComp {
  val component = ScalaComponent.builder[Unit]("PlaceholderComp")
    .render(_ => <.h2("placeholder"))
    .build

  def apply() = SPWidget(spwb => component())
}


object SectionList {
  // To be loaded from the backend soon!
  val sections = List(
    "gul",
    "blå"
  )
}
