package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import spgui.circuit.{ SPGUICircuit, CloseWidget }
import spgui.components.Icon

object DashboardItem {
  case class Props(element: VdomElement, widgetType: String, id: java.util.UUID)
  private val component = ScalaComponent.build[Props]("Widget")
    .render_P(props =>
      <.div(
        ^.className := DashboardCSS.widgetPanel.htmlClass,
        ^.className := "panel panel-default",
        <.div(
          ^.className := DashboardCSS.widgetPanelHeader.htmlClass,
//          <.div(props.widgetType),
          <.div(
            ^.className := DashboardCSS.closeButton.htmlClass,
                      ^.className := "btn",
            Icon.close,
            ^.onClick --> Callback(SPGUICircuit.dispatch(CloseWidget(props.id))
            )
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
    ).build


  def apply(element: VdomElement, widgetType: String,id: java.util.UUID) =
    component(Props(element, widgetType, id))
}
