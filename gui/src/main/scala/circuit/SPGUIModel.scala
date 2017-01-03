package spgui.circuit

import diode._
import japgolly.scalajs.react.ReactElement

// state
case class RootModel(content: ReactElement, openWidgets: List[ReactElement])

// actions
case object AddWidget extends Action
case class SetContent(content: ReactElement) extends Action
