package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria
import scalacss.ScalaCssReact._

import spgui.circuit.{ SPGUICircuit, CloseWidget, CollapseWidgetToggle }
import spgui.components.{Icon}



object DashboardItem {
  case class Props(element: VdomElement, widgetType: String, id: java.util.UUID)
  case class State(hiddenMenuBar: Boolean = false)

  class DashboardItemBackend($: BackendScope[Props, State]){
    def ToggleMenuBar(e :ReactEventFromInput): CallbackTo[Unit] = { //ugly..
      $.state >>= (s => (
        if(s.hiddenMenuBar) $.modState(_.copy(hiddenMenuBar = false))
        else $.modState(_.copy(hiddenMenuBar = true))
      ))
    }

    def render (p: Props, s:State) =
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
            if(s.hiddenMenuBar)Icon.arrowDown
            else Icon.arrowUp
          ),
          if(s.hiddenMenuBar){DashboardCSS.widgetPanelHidden}
          else {EmptyVdom}
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

  private val component = ScalaComponent.builder[Props]("DashboardItem")
    .initialStateFromProps(p => State())
    .renderBackend[DashboardItemBackend]
    .build

  def apply(element: VdomElement, widgetType: String,id: java.util.UUID) =
    component(Props(element, widgetType, id))

}
