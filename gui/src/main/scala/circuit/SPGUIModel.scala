package spgui.circuit

import diode._
import japgolly.scalajs.react.ReactElement

// state
case class RootModel(availableWidgets: List[String], openWidgets: List[String])

// actions
case class AddWidget(widgetType: String) extends Action
case class CloseWidget(index: Int) extends Action
