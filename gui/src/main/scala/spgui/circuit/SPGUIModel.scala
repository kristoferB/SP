package spgui.circuit

import diode._
import java.util.UUID
import sp.domain.SPValue

// state
case class SPGUIModel(openWidgets: OpenWidgets = OpenWidgets(), state: FrontEndState = FrontEndState())

case class OpenWidgets(xs: Map[UUID, OpenWidget] = Map())
case class OpenWidget(id: UUID, layout: WidgetLayout, widgetType: String, data: SPValue = SPValue.empty)
case class WidgetLayout(x: Int, y: Int, w: Int, h: Int)

case class FrontEndState(currentModel: Option[UUID] = None,
                         selectedItems: List[UUID] = List(),
                         userID: Option[UUID] = None,
                         clientID: UUID = UUID.randomUUID()
                        )

// actions
case class AddWidget(widgetType: String, width: Int = 2, height: Int = 2, initialData: SPValue = SPValue.empty, id: UUID = UUID.randomUUID()) extends Action
case class CloseWidget(id: UUID) extends Action
case object CloseAllWidgets extends Action
case class SetWidgetData(id: UUID, data: SPValue) extends Action
case class UpdateLayout(id: UUID, newLayout: WidgetLayout) extends Action
case class UpdateFrontEndState(state: FrontEndState) extends Action

// used when failing to retrieve a state from browser storage
object InitialState {
  def apply() = SPGUIModel()
}
