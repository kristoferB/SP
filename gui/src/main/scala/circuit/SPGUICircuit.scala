package spgui.circuit

import diode._
import diode.react.ReactConnector
import japgolly.scalajs.react.ReactElement

import upickle.default._
import org.scalajs.dom.ext.SessionStorage

object SPGUICircuit extends Circuit[RootModel] with ReactConnector[RootModel] {
  def initialModel = InitialState()
  val actionHandler = composeHandlers(
    new DashboardHandler(zoomRW(_.openWidgets)((m,v) => m.copy(openWidgets = v)))
  )
}

object BrowserPersistenceStorage {
  val namespace = "ey"
  def store(openWidgets: List[String]) = SessionStorage(namespace) = write(openWidgets)
}

/*
class BrowserPersistenceHandler[M](modelRW: ModelRW[M, SPGUIModel]) extends ActionHandler(modelRW) {
  val namespace = "ey"
  def handle = {
    case Action => updated{
      SessionStorage(namespace) = write(value)
      value
    }
  }
}
 */

class DashboardHandler[M](modelRW: ModelRW[M, List[String]]) extends ActionHandler(modelRW) {
  def handle = {
    case AddWidget(widgetType) => {
      BrowserPersistenceStorage.store(value)
      updated(value :+ widgetType)
    }
    case CloseWidget(index) => updated(value.zipWithIndex.filter(_._2 != index).map(_._1))
  }
}

