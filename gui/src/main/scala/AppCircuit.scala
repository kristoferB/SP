package spgui

import diode._
import diode.react.ReactConnector
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

case object AddWidget extends Action
case class SetContent(content: ReactElement) extends Action

case class RootModel(content: ReactElement, openWidgets: List[ReactElement])


object AppCircuit extends Circuit[RootModel] with ReactConnector[RootModel] {
  def initialModel = RootModel(PlaceholderComp(), List(PlaceholderComp()))
  val actionHandler = composeHandlers(
    new DashboardHandler(zoomRW(_.openWidgets)((m,v) => m.copy(openWidgets = v))),
    new ContentHandler(zoomRW(_.content)((m,v) => m.copy(content = v)))
  )
}

class DashboardHandler[M](modelRW: ModelRW[M, List[ReactElement]]) extends ActionHandler(modelRW) {
  def handle = { case AddWidget => updated(SomeWidget() :: value) }
}

class ContentHandler[M](modelRW: ModelRW[M, ReactElement]) extends ActionHandler(modelRW) {
  def handle = { case SetContent(content) => updated(content) }
}

// stuff below here doesn't belong to the circuit, just setting initial state to something
object PlaceholderComp {
  val component = ReactComponentB[Unit]("PlaceholderComp")
    .render(_ => <.h2("placeholder"))
    .build

  def apply() = component()
}

object SomeWidget {
  val component = ReactComponentB[Unit]("SomeWidget")
    .render(_ => <.h2("SomeWidget"))
    .build

  def apply() = component()
}
