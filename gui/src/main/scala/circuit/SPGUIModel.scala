package spgui.circuit

import diode._

// state
case class SPGUIModel(openWidgets: List[OpenWidget], layout: Int)
case class OpenWidget(layout: WidgetLayout, widgetType: String, stringifiedWidgetData: String = "")
case class WidgetLayout(x: Int, y: Int, w: Int, h: Int)

// actions
case class AddWidget(widgetType: String) extends Action
case class CloseWidget(index: Int) extends Action
case class SetWidgetData(index: Int, stringifiedWidgetData: String) extends Action
case class UpdateLayout(newLayout: String) extends Action

case class LayoutUpdated(layout: Int) extends Action

// used when failing to retrieve a state from browser storage
object InitialState {
  val initialState = SPGUIModel(
    openWidgets = List(
      OpenWidget(WidgetLayout(0,0,1,1), "PlcHldrC")
    ),
    layout = 0
  )
  def apply() = initialState
}
