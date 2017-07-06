package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

import spgui.circuit.{ SPGUICircuit, CloseWidget, CollapseWidgetToggle }
import spgui.components.{Icon}

object DashboardItem {
  case class Props(
    element: VdomElement,
    widgetType: String,
    id: java.util.UUID,
    panelHeight: Int
  )

  val showHeaders = SPGUICircuit.zoom(_.settings.showHeaders)

  class DashboardItemBackend($: BackendScope[Props, Unit]){
    def render (p: Props) = {
      <.div(
        DashboardCSS.widgetPanel,        
        <.div(
          ^.className := "modal-header",
          DashboardCSS.widgetPanelHeader,
          <.h5(
            DashboardCSS.widgetPanelLabel, p.widgetType),
          <.a(
            ^.className := "close",
            ^.onClick --> Callback(SPGUICircuit.dispatch(CloseWidget(p.id))),
            Icon.close,
            DashboardCSS.widgetPanelButton
          ),
          <.a(
            VdomAttr("data-toggle") := "tooltip",
            VdomAttr("title") := "toggle panel",
            ^.className := "close",
            ^.onClick --> Callback(SPGUICircuit.dispatch(
              CollapseWidgetToggle(p.id)
            )),
            DashboardCSS.widgetPanelButton,
            if(p.panelHeight == 1)Icon.arrowDown
            else Icon.arrowUp
          ),
          {
            if(!showHeaders.value) ^.className:= DashboardCSS.widgetPanelHidden.htmlClass
            else {EmptyVdom}
          }
        ),
        <.div(
          ^.className := DashboardCSS.widgetPanelBody.htmlClass,
          <.div(
            ^.className := "panel-body",
            ^.className := DashboardCSS.widgetPanelContent.htmlClass,
            p.element
          )
        )
      )
    }
  }

  private val component = ScalaComponent.builder[Props]("DashboardItem")
    .renderBackend[DashboardItemBackend]
    .build

  def apply(element: VdomElement, widgetType: String,id: java.util.UUID, panelHeight: Int) =
    component(Props(element, widgetType, id, panelHeight))

}
