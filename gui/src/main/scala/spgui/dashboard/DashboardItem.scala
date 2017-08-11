package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import spgui.circuit.{ SPGUICircuit, CloseWidget }
import spgui.components.{Icon,SPButton}

object DashboardItem {
  case class Props(element: VdomElement, widgetType: String, id: java.util.UUID)
  private val component = ScalaComponent.builder[Props]("Widget")
    .render_P(props =>
      <.div(
        ^.className := DashboardCSS.widgetPanel.htmlClass,
        ^.className := "panel panel-default",
        <.div(
          ^.className := "modal-header " + DashboardCSS.widgetPanelHeader.htmlClass,
          <.h5(^.className:="modal-title "+ DashboardCSS.widgetPanelLabel.htmlClass, props.widgetType),
            <.a(^.className := "close",
                ^.onClick --> Callback(SPGUICircuit.dispatch(CloseWidget(props.id))),
                Icon.close
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
