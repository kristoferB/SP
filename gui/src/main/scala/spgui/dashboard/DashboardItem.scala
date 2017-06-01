package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria
import scalacss.ScalaCssReact._

import spgui.circuit.{ SPGUICircuit, CloseWidget }
import spgui.components.{Icon,SPButton}

object DashboardItem {
  case class Props(element: ReactElement, widgetType: String, id: java.util.UUID)
  case class State(hiddenMenuBar: Boolean = false)

class DashboardItemBackend($: BackendScope[Props, State]){
    def ToggleMenuBar(e :ReactEventI): CallbackTo[Unit] = { //ugly..
      $.state >>= (s => (
        if(s.hiddenMenuBar) $.modState(_.copy(hiddenMenuBar = false))
        else $.modState(_.copy(hiddenMenuBar = true))
      ))
    }

  def render (p: Props, s:State) =
      <.div(
        ^.className := DashboardCSS.widgetPanel.htmlClass,
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
                ReactAttr.Generic("data-toggle") := "tooltip",
                ReactAttr.Generic("title") := "toggle panel",
                ^.className := "close",
                ^.onClick ==>  ToggleMenuBar,
                DashboardCSS.widgetPanelButton,
                if(s.hiddenMenuBar)Icon.arrowDown
                else Icon.arrowUp
              ),
              if(s.hiddenMenuBar){DashboardCSS.widgetPanelHidden}
              else {None}
          ),
        <.div(
          ^.className := DashboardCSS.widgetPanelBody.htmlClass,
          <.div(
            ^.className := "panel-body",
            ^.className := DashboardCSS.widgetPanelContent.htmlClass,
            p.element)
        )
      )
}

  private val component = ReactComponentB[Props]("DashboardItem")
    .initialState_P(p => State())
    .renderBackend[DashboardItemBackend]
    .build


  def apply(element: ReactElement, widgetType: String,id: java.util.UUID) =
    component(Props(element, widgetType, id))

}
