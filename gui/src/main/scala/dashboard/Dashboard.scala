package spgui.dashboard

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import diode.react.ModelProxy

import spgui.WidgetList

object Dashboard {
  case class Props(proxy: ModelProxy[List[String]])

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props) =
      <.div(
        ^.className := DashboardCSS.dashboardBackground.htmlClass,
        ReactGridLayout(
          width = 1920,
          cols = 8,
          draggableHandle = "." + DashboardCSS.widgetPanelHeader.htmlClass,
          onLayoutChange = _ => println("hej"),
          for((widgetType,index) <- p.proxy().zipWithIndex)
          yield ReactGridLayoutItem(
            key = index.toString,
            i = "idkdk",
            x = 0,
            y = 0,
            w = 1,
            h = 1,
            isDraggable = true,
            isResizable = true,
            child = DashboardItem(WidgetList()(widgetType), index)
          )
        )
      )
  }

  private val component = ReactComponentB[Props]("Dashboard")
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[List[String]]) = component(Props(proxy))
}
