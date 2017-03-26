package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import spgui.circuit.{ SPGUICircuit, CloseWidget }
import spgui.components.Icon

import spgui.circuit.SPGUICircuit

object DashboardItem {
  val compactMode = SPGUICircuit.zoom(_.settings.compact)

  case class Props(element: ReactElement, widgetType: String, id: java.util.UUID)
  private val component = ReactComponentB[Props]("Widget")
    .render_P(props =>
    <.div(
      ^.className := DashboardCSS.widgetPanel.htmlClass,
      ^.className := "panel panel-default",
      compactMode.value match {
        case false =>
          <.div(
            ^.className := DashboardCSS.widgetPanelHeader.htmlClass,
            <.div(
              ^.className := DashboardCSS.closeButton.htmlClass,
              ^.className := "btn",
              Icon.close,
              ^.onClick --> Callback(SPGUICircuit.dispatch(CloseWidget(props.id))
              )
            )
          )
        case true => None
      },
      <.div(
        ^.className := DashboardCSS.widgetPanelBody.htmlClass,
        <.div(
          ^.className := "panel-body",
          ^.className := DashboardCSS.widgetPanelContent.htmlClass,
          props.element)
      )
    )
  ).build


  def apply(element: ReactElement, widgetType: String,id: java.util.UUID) =
    component(Props(element, widgetType, id))
}
