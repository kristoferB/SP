package spgui.circuit

import diode._
import diode.react.ReactConnector

import upickle.default._
import org.scalajs.dom.ext.SessionStorage
import scala.util.{Success, Try}
import org.scalajs.dom.console

object SPGUICircuit extends Circuit[SPGUIModel] with ReactConnector[SPGUIModel] {
  def initialModel = BrowserStorage.load.getOrElse(InitialState())
  val actionHandler = composeHandlers(
    new DashboardHandler(zoomRW(_.openWidgets)((m,v) => m.copy(openWidgets = v)))
  )
  // store state upon any model change
  subscribe(zoomRW(myM => myM)((m,v) => v))(m => BrowserStorage.store(m.value))
}

class DashboardHandler[M](modelRW: ModelRW[M, OpenWidgets]) extends ActionHandler(modelRW) {
  def handle = {
    case AddWidget(widgetType) =>
      updated(OpenWidgets(value.count + 1, value.list :+ OpenWidget(value.count + 1, WidgetLayout(0,0,1,1), widgetType)))
    case CloseWidget(id) =>
      updated(OpenWidgets(value.count, value.list.filter(_.id != id)))
    case SetWidgetData(id, stringifiedWidgetData) =>
      updated(OpenWidgets(value.count, value.list.map(ow => if(ow.id == id) ow.copy(stringifiedWidgetData = stringifiedWidgetData) else ow)))
    case LayoutUpdated(id, newLayout) => {
      updated(OpenWidgets(value.count, value.list.map(ow => if(ow.id == id) ow.copy(layout = newLayout) else ow)))
    }
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
