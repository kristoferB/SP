package spgui.circuit

import diode._

// state
case class SPGUIModel(openWidgets: OpenWidgets, layout: Int)
case class OpenWidgets(count: Int, list: List[OpenWidget])
case class OpenWidget(id: Int, widgetType: String, stringifiedWidgetData: String = "")

// actions
case class AddWidget(widgetType: String) extends Action
case class CloseWidget(id: Int) extends Action
case class SetWidgetData(index: Int, stringifiedWidgetData: String) extends Action

case class LayoutUpdated(layout: Int) extends Action

// used when failing to retrieve a state from browser storage
object InitialState {
  val initialState = SPGUIModel(
    openWidgets = OpenWidgets(1, List(
      OpenWidget(1, "PlcHldrC")
    )),
    layout = 0
  )
  def apply() = initialState
}
