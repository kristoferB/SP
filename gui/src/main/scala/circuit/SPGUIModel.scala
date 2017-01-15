package spgui.circuit

import diode._

// state
case class SPGUIModel(openWidgets: List[String])

// actions
case class AddWidget(widgetType: String) extends Action
case class CloseWidget(index: Int) extends Action

// used when failing to retrieve a state from browser storage
object InitialState {
  val initialState = SPGUIModel(openWidgets = List("PlcHldrC"))
  def apply() = initialState
}
