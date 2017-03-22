package spgui.circuit

import diode._
import diode.react.ReactConnector
import org.scalajs.dom.ext.LocalStorage

import scala.util.{Success, Try}
import spgui.theming.Theme

object SPGUICircuit extends Circuit[SPGUIModel] with ReactConnector[SPGUIModel] {
  def initialModel = BrowserStorage.load.getOrElse(InitialState())
  val actionHandler = composeHandlers(
    new DashboardHandler(zoomRW(_.openWidgets)((m,v) => m.copy(openWidgets = v))),
    new GlobalStateHandler(zoomRW(_.globalState)((m, v) => m.copy(globalState = v))),
    new SettingsHandler(zoomRW(_.settings)((m, v) => m.copy(settings = v))),
    new WidgetDataHandler(zoomRW(_.widgetData)((m,v) => m.copy(widgetData = v)))
  )
  // store state upon any model change
  subscribe(zoomRW(myM => myM)((m,v) => v))(m => BrowserStorage.store(m.value))
}

class DashboardHandler[M](modelRW: ModelRW[M, OpenWidgets]) extends ActionHandler(modelRW) {
  override def handle = {
    case AddWidget(widgetType, width, height, id) =>
      val rightmost = value.xs.values.foldLeft(0)((a, b) => math.max(a, b.layout.w + b.layout.x))
      val newWidget = OpenWidget(id, WidgetLayout(rightmost, 0, width, height), widgetType)
      updated(OpenWidgets(value.xs + (id -> newWidget)))
    case CloseWidget(id) =>
      updated(OpenWidgets(value.xs - id))
    case CloseAllWidgets => updated(OpenWidgets())
    case UpdateLayout(id, newLayout) => {
      val updW = value.xs.get(id)
        .map(_.copy(layout = newLayout))
        .map(x => value.xs + (x.id -> x))
        .getOrElse(value.xs)
      updated(OpenWidgets(updW))
    }
  }
}

class GlobalStateHandler[M](modelRW: ModelRW[M, GlobalState]) extends ActionHandler(modelRW) {
  override def handle = {
    case UpdateGlobalState(state) =>
      updated(state)
  }
}

class WidgetDataHandler[M](modelRW: ModelRW[M, WidgetData]) extends ActionHandler(modelRW) {
  override def handle = {
    case UpdateWidgetData(id, d) =>
      val updW = value.xs + (id -> d)
      updated(WidgetData(updW))
  }
}

class SettingsHandler[M](modelRW: ModelRW[M, Settings]) extends ActionHandler(modelRW) {
  override def handle = {
    case SetTheme(newTheme) => {
      updated(value.copy(
        theme = newTheme
      ))
    }
  }
}

import sp.messages.Pickles._
object BrowserStorage {
  val namespace = "SPGUIState"
  def store(spGUIState: SPGUIModel) = LocalStorage(namespace) = write(spGUIState)
  def load: Option[SPGUIModel] =
    Try(LocalStorage(namespace) map read[SPGUIModel]) match {
      case Success(Some(state)) => Some(state)
      case _ => None
    }
}
