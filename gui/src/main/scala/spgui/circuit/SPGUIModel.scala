package spgui.circuit

import diode._
import java.util.UUID
import sp.domain.SPValue
import spgui.theming.Theming.Theme

// state
case class SPGUIModel(
  openWidgets: OpenWidgets = OpenWidgets(),
  globalState: GlobalState = GlobalState(),
  widgetData: WidgetData = WidgetData(Map()),
  settings: Settings = Settings(),
  draggingState: DraggingState = DraggingState()
)

case class OpenWidgets(xs: Map[UUID, OpenWidget] = Map())
case class OpenWidget(id: UUID, layout: WidgetLayout, widgetType: String)
case class WidgetLayout(x: Int, y: Int, w: Int, h: Int, collapsedHeight: Int = 1)

case class GlobalState(
  currentModel: Option[UUID] = None,
  selectedItems: List[UUID] = List(),
  userID: Option[UUID] = None,
  clientID: UUID = UUID.randomUUID()
)
case class WidgetData(xs: Map[UUID, SPValue])

case class Settings(
  theme: Theme = Theme(),
  showHeaders: Boolean = true 
)

case class DropEventData(droppedId: UUID, targetId: UUID)
case class DraggingState(
  target: UUID = null,
  dragging: Boolean = false,
  renderStyle: String = "",
  data: String = "",
  latestDropEvent: DropEventData = DropEventData(null, null) 
)

// actions
case class AddWidget(widgetType: String, width: Int = 2, height: Int = 2, id: UUID = UUID.randomUUID()) extends Action
case class CloseWidget(id: UUID) extends Action
case class CollapseWidgetToggle(id: UUID) extends Action
case object CloseAllWidgets extends Action
case class UpdateWidgetData(id: UUID, data: SPValue) extends Action
case class UpdateLayout(id: UUID, newLayout: WidgetLayout) extends Action
case class SetLayout(layout: Map[UUID, WidgetLayout]) extends Action
case class UpdateGlobalState(state: GlobalState) extends Action
case class SetTheme(theme: Theme) extends Action
case object ToggleHeaders extends Action

case class SetDraggableRenderStyle(style:String) extends Action
case class SetDraggableData(data: String) extends Action
case class SetCurrentlyDragging(enabled: Boolean) extends Action 
case class SetDraggingTarget(id: UUID) extends Action
case object UnsetDraggingTarget extends Action
case class DropEvent(dropped: UUID, target: UUID) extends Action

// used when failing to retrieve a state from browser storage
object InitialState {
  def apply() = SPGUIModel()
}
