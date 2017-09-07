package spgui

import japgolly.scalajs.react.vdom.html_<^.VdomElement
import spgui._

object LoadingWidgets {

  type Widget = (String, SPWidgetBase => VdomElement, Int, Int)

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


  def loadWidgets: Unit = {
    WidgetList.addWidgets(erica)
  }
}






