package spgui.circuit

import diode._
import diode.react.ReactConnector
import org.scalajs.dom.ext.LocalStorage

import scala.util.{Success, Try}
import spgui.theming.Theming.Theme
import spgui.dashboard.Dashboard

object SPGUICircuit extends Circuit[SPGUIModel] with ReactConnector[SPGUIModel] {
  def initialModel = BrowserStorage.load.getOrElse(InitialState())
  val actionHandler = composeHandlers(
    new DashboardHandler(zoomRW(_.openWidgets)((m,v) => m.copy(openWidgets = v))),
    new GlobalStateHandler(zoomRW(_.globalState)((m, v) => m.copy(globalState = v))),
    new SettingsHandler(zoomRW(_.settings)((m, v) => m.copy(settings = v))),
    new WidgetDataHandler(zoomRW(_.widgetData)((m,v) => m.copy(widgetData = v))),
    new DraggingHandler(zoomRW(_.draggingState)((m,v) => m.copy(draggingState = v)))

  )
  // store state upon any model change
  subscribe(zoomRW(myM => myM)((m,v) => v))(m => BrowserStorage.store(m.value))
}

class DashboardHandler[M](modelRW: ModelRW[M, OpenWidgets]) extends ActionHandler(modelRW) {
  override def handle = {
    case AddWidget(widgetType, width, height, id) =>
      val occupiedGrids = value.xs.values.map(w =>
        for{x <- w.layout.x to w.layout.x + w.layout.w-1} yield {
          for{y <- w.layout.y to w.layout.y + w.layout.h-1} yield {
            (x, y)
          }
        }
      ).flatten.flatten
      val bestPosition:Int = Stream.from(0).find(i => {
        val x = i % Dashboard.cols
        val y = i / Dashboard.cols

        val requiredGrids = (for{reqX <- x to x + width -1} yield {
          for{reqY <- y to y + height-1} yield {
            (reqX, reqY)
          }
        }).toSeq.flatten
        requiredGrids.forall(req =>
          occupiedGrids.forall(occ =>
            !(occ._1 == req._1 && occ._2 == req._2 || req._1 >= Dashboard.cols)
          )
        )
      }).get
      val x:Int = bestPosition % Dashboard.cols
      val y:Int = bestPosition / Dashboard.cols
      val newWidget = OpenWidget(
        id,
        WidgetLayout(x, y, width, height),
        widgetType
      )
      updated(OpenWidgets(value.xs + (id -> newWidget)))
    case CloseWidget(id) =>
      updated(OpenWidgets(value.xs - id))
    case CollapseWidgetToggle(id) =>
      val targetWidget = value.xs.get(id).get
      val modifiedWidget = targetWidget.layout.h match {
        case 1 => targetWidget.copy(
          layout = targetWidget.layout.copy(
            collapsedHeight = 1,
            h = targetWidget.layout.collapsedHeight match {
              // this deals with the fact that panels can already have a height of 1
              // it would be strange to "restore" the height to the current height
              case 1 => 4
              case _ => targetWidget.layout.collapsedHeight
            }
          )
        )
        case _ => targetWidget.copy(
          layout = targetWidget.layout.copy(
            collapsedHeight = targetWidget.layout.h ,
            h = 1
          )
        )
      }
      updated(OpenWidgets((value.xs - id ) + (id -> modifiedWidget)))
    case CloseAllWidgets => updated(OpenWidgets())
    case UpdateLayout(id, newLayout) => {
      val updW = value.xs.get(id)
        .map(_.copy(layout = newLayout))
        .map(x => value.xs + (x.id -> x))
        .getOrElse(value.xs)
      updated(OpenWidgets(updW))
    }
    case SetLayout(newLayout) => {
      val updW = OpenWidgets(value.xs.map(x =>
        (
          x._1,
          x._2.copy(
            layout = newLayout(x._1)
          )
        )
      ))
      updated(updW)
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
    case ToggleHeaders => {
      updated(value.copy(
        showHeaders = !value.showHeaders
      ))
    }
  }
}

class DraggingHandler[M](modelRW: ModelRW[M, DraggingState]) extends ActionHandler(modelRW) {
  override def handle = {
    case  SetMousePosition(x, y) => {
      updated(value.copy(x=x, y=y))
    }
    case SetDraggableRenderStyle(renderStyle) => updated(value.copy(renderStyle = renderStyle))
    case SetDraggableData(data) => updated(value.copy(data = data))
    case SetCurrentlyDragging(dragging) => updated((value.copy(dragging = dragging)))
    case SetDraggingTarget(id) => updated((value.copy(target = id)))
    case UnsetDraggingTarget(id) =>
      if(value.target == id || true) updated((value.copy(target = null)))
      else updated(value)
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
