package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import diode.react.ModelProxy
import spgui.circuit.OpenWidget

import spgui.WidgetList
import spgui.circuit.{SPGUICircuit, UpdateLayout}
import org.scalajs.dom.console

import scala.scalajs.js

object Dashboard {
  case class Props(proxy: ModelProxy[List[OpenWidget]])

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props) =
      <.div(
        ^.className := DashboardCSS.dashboardBackground.htmlClass,
        ReactGridLayout(
          width = 1920,
          cols = 8,
          draggableHandle = "." + DashboardCSS.widgetPanelHeader.htmlClass,
          onLayoutChange = (layout => {
            layout.asInstanceOf[LayoutData].foreach(
              element => console.log(element)
            )
          }),
          for((openWidget,index) <- p.proxy().zipWithIndex)
          yield ReactGridLayoutItem(
            key = index.toString,
            i = "idkdk",
            x = openWidget.layout.x,
            y = openWidget.layout.y,
            w = openWidget.layout.w,
            h = openWidget.layout.h,
            isDraggable = true,
            isResizable = true,
            child = DashboardItem(WidgetList()(openWidget.widgetType), index)
          )
        )
      )
  }

  private val component = ReactComponentB[Props]("Dashboard")
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[List[OpenWidget]]) = component(Props(proxy))
}
