package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.circuit.{SPGUICircuit, CloseWidget}

object DashboardItem {
  case class Props(element: ReactElement, id: Int)
  private val component = ReactComponentB[Props]("Widget")
    .render_P(props =>
    <.div(
      ^.className := DashboardCSS.widgetPanel.htmlClass,
      ^.className := "panel panel-default",
      <.div(
        ^.className := DashboardCSS.widgetPanelHeader.htmlClass,
        <.button(
          ^.className := "btn btn-default",
          <.i(^.className := "icon-remove" ),
          "close me",
          ^.onClick --> Callback(SPGUICircuit.dispatch(CloseWidget(props.id)))

        )
      ),
      <.div(
        ^.className := DashboardCSS.widgetPanelBody.htmlClass,
        <.div(
          ^.className := "panel-body",
          ^.className := DashboardCSS.widgetPanelContent.htmlClass,
          props.element)
      )
    )
  )
    .build
  def apply(element: ReactElement, id: Int) = component(Props(element, id))
}
