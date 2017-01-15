package spgui.circuit

import diode._

// state
case class SPGUIModel(openWidgets: List[OpenWidget])
case class OpenWidget(widgetType: String, stringifiedWidgetData: String = "")

// actions
case class AddWidget(widgetType: String) extends Action
case class CloseWidget(index: Int) extends Action
case class SetWidgetData(index: Int, stringifiedWidgetData: String) extends Action

// used when failing to retrieve a state from browser storage
object InitialState {
  val initialState = SPGUIModel(
    openWidgets = List(
      OpenWidget("PlcHldrC")
    )
  )
  def apply() = initialState
}
