package spgui.circuit

import diode._
import diode.react.ReactConnector
import japgolly.scalajs.react.ReactElement

object SPGUICircuit extends Circuit[RootModel] with ReactConnector[RootModel] {
  def initialModel = RootModel(List(PlaceholderComp()))
  val actionHandler = composeHandlers(
    new DashboardHandler(zoomRW(_.openWidgets)((m,v) => m.copy(openWidgets = v)))
  )
}

class DashboardHandler[M](modelRW: ModelRW[M, List[ReactElement]]) extends ActionHandler(modelRW) {
  def handle = {
    case AddWidget(element) => updated(element :: value)
    case CloseWidget(index) => updated(value.zipWithIndex.filter(_._2 != index).map(_._1))
  }
}
