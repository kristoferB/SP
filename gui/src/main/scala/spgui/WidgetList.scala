package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._


object WidgetList {
  val list =
    List[(String, SPWidgetBase => ReactElement, Int, Int)](
      ("Klocka", widgets.ClockWidget(), 2, 2),

      // Team widgets -->
      ("Triagediagram", widgets.TriageWidget(), 2, 8),
      ("Statusdiagram", widgets.StatusWidget(), 2, 3),
      ("Platsdiagram", widgets.PlaceWidget(), 2, 3),
      ("Patientkort", widgets.PatientCardsWidget(), 5, 19),
      ("Lång tid sedan händelse", widgets.PatientReminderWidget(), 1, 19),
      // <--

      // Coordinator widgets -->
      ("Rumskarta (koordinator)", widgets.RoomOverviewServiceWidget(), 15, 4),
      ("Triage- och statusdiagram (koordinator)", widgets.CoordinatorDiagramServiceWidget(), 17, 3),
      ("Väntrumsdiagram (koordinator)", widgets.WaitingRoomServiceWidget(), 4, 3)
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
