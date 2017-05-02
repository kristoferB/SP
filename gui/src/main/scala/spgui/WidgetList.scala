package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spgui.widgets.examples._


object WidgetList {
  val list =
    List[(String, SPWidgetBase => ReactElement, Int, Int)](
      ("Klocka", widgets.ClockWidget(), 3, 1),

      // Team widgets -->
      ("Triagediagram", widgets.TriageWidget(), 3, 3),
      ("Statusdiagram", widgets.StatusWidget(), 3, 1),
      ("Platsdiagram", widgets.PlaceWidget(), 3, 1),
      ("Patientkort", widgets.PatientCardsWidget(), 7, 5),
      ("Lång tid sedan händelse", widgets.PatientReminderWidget(), 3, 3),
      // <--

      // Coordinator widgets -->
      ("Rumskarta (koordinator)", widgets.RoomOverviewServiceWidget(), 3, 3),
      ("Triage- och statusdiagram (koordinator)", widgets.CoordinatorDiagramServiceWidget(), 3, 3),
      ("Väntrumsdiagram (koordinator)", widgets.WaitingRoomServiceWidget(), 3, 3)
      // <--
    )

  val map = list.map(t => t._1 -> (t._2, t._3, t._4)).toMap
}


object SectionList {
  // To be loaded from the backend soon!
  val sections = List(
    "gul",
    "blå"
  )
}
