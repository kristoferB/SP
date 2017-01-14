package spgui.circuit

import diode._
import diode.react.ReactConnector
import japgolly.scalajs.react.ReactElement

object SPGUICircuit extends Circuit[RootModel] with ReactConnector[RootModel] {
  def initialModel = InitialState()
  val actionHandler = composeHandlers(
    new DashboardHandler(zoomRW(_.openWidgets)((m,v) => m.copy(openWidgets = v)))
  )
}

class DashboardHandler[M](modelRW: ModelRW[M, List[String]]) extends ActionHandler(modelRW) {
  def handle = {
    case AddWidget(widgetType) => updated(value :+ widgetType)
    case CloseWidget(index) => updated(value.zipWithIndex.filter(_._2 != index).map(_._1))
  }
}
