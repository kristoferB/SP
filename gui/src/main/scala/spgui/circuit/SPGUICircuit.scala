package spgui.circuit

import diode._
import diode.react.ReactConnector
import org.scalajs.dom.ext.LocalStorage

import scala.util.{Success, Try}
import scala.math._
import java.util.UUID

object SPGUICircuit extends Circuit[SPGUIModel] with ReactConnector[SPGUIModel] {
  def initialModel = BrowserStorage.load.getOrElse(InitialState())
  val actionHandler = composeHandlers(
    new DashboardHandler(zoomRW(_.openWidgets)((m,v) => m.copy(openWidgets = v))),
    new FrontEndStateHandler(zoomRW(_.state)((m,v) => m.copy(state = v)))
  )
  // store state upon any model change
  subscribe(zoomRW(myM => myM)((m,v) => v))(m => BrowserStorage.store(m.value))
}

class DashboardHandler[M](modelRW: ModelRW[M, OpenWidgets]) extends ActionHandler(modelRW) {
  override def handle = {
    case AddWidget(widgetType, width, height, data, id) =>
      val rightmost = value.xs.values.foldLeft(0)((a, b) => math.max(a, b.layout.w + b.layout.x))
      val newWidget = OpenWidget(id, WidgetLayout(rightmost, 0, width, height), widgetType, data)
      updated(OpenWidgets(value.xs + (id -> newWidget)))
    case CloseWidget(id) =>
      updated(OpenWidgets(value.xs - id))
    case CloseAllWidgets => updated(OpenWidgets(Map()))
    case SetWidgetData(id, d) =>
      val updW = value.xs.get(id)
        .map(_.copy(data = d))
        .map(x => value.xs + (x.id -> x))
        .getOrElse(value.xs)
      updated(OpenWidgets(updW))
    case UpdateLayout(id, newLayout) => {
      val updW = value.xs.get(id)
        .map(_.copy(layout = newLayout))
        .map(x => value.xs + (x.id -> x))
        .getOrElse(value.xs)
      updated(OpenWidgets(updW))
    }
  }
}

class FrontEndStateHandler[M](modelRW: ModelRW[M, FrontEndState]) extends ActionHandler(modelRW) {
  override def handle = {
    case UpdateFrontEndState(state) =>
      updated(state)
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
