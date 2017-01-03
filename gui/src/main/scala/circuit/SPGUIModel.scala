package spgui.circuit

import diode._
import japgolly.scalajs.react.ReactElement

// state
case class RootModel(content: ReactElement, openWidgets: List[ReactElement])

// actions
case class AddWidget(element: ReactElement) extends Action
case class CloseWidget(index: Int) extends Action
case class SetContent(content: ReactElement) extends Action
