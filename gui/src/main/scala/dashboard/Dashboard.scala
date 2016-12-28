package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.Grid.{ReactGridLayoutFacade, LayoutItem}

object Dashboard {

  val elemA = <.div("Hello from element A")
  val elemB = <.div("Hello from element B")

  case class State(openWidgets: List[ReactElement])

  private class Backend($: BackendScope[Unit, State]) {
    def addWidget(w: ReactElement) = $.modState(s => State(w :: s.openWidgets))
    def removeWidget() = $.modState(s => State(s.openWidgets.tail))
    def render(s: State) =
      <.div(
        <.h3("hello from Dashboard"),
        <.button("Add an elem A", ^.onClick --> addWidget(elemA)),
        <.button("Add an elem B", ^.onClick --> addWidget(elemB)),
        <.button("Remove an element", ^.onClick --> removeWidget()),
        ReactGridLayoutFacade(width=1920,
                              onLayoutChange = _ => println("hej")
        ).apply(
          for((w,i) <- s.openWidgets.zipWithIndex) yield <.div(^.key := i, w)
        )
    )
  }

  private val component = ReactComponentB[Unit]("Dashboard")
    .initialState(State(List(elemA, elemB)))
    .renderBackend[Backend]
    .build

  def apply(): ReactElement = component()
}

