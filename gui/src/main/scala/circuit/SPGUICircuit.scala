package spgui.circuit

import diode._
import diode.react.ReactConnector

import upickle.default._
import org.scalajs.dom.ext.SessionStorage
import scala.util.{Success, Try}

object SPGUICircuit extends Circuit[SPGUIModel] with ReactConnector[SPGUIModel] {
  def initialModel = BrowserStorage.load.getOrElse(InitialState())
  val actionHandler = composeHandlers(
    new DashboardHandler(zoomRW(_.openWidgets)((m,v) => m.copy(openWidgets = v)))
  )
  // store state upon any model change
  subscribe(zoomRW(myM => myM)((m,v) => v))(m => BrowserStorage.store(m.value))
}

class DashboardHandler[M](modelRW: ModelRW[M, List[String]]) extends ActionHandler(modelRW) {
  def handle = {
    case AddWidget(widgetType) => updated(value :+ widgetType)
    case CloseWidget(index) => updated(value.zipWithIndex.filter(_._2 != index).map(_._1))
  }
}

object BrowserStorage {
  val namespace = "SPGUIState"
  def store(spGUIState: SPGUIModel) = SessionStorage(namespace) = write(spGUIState)
  def load: Option[SPGUIModel] =
    Try(SessionStorage(namespace) map read[SPGUIModel]) match {
      case Success(Some(state)) => Some(state)
      case _ => None
    }
}
