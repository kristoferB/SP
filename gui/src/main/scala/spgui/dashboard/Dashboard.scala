package spgui.dashboard

import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import diode.react.ModelProxy

import scalajs.js.Dynamic
import scalajs.js.JSON
import spgui.SPWidgetBase
import spgui.circuit._
import spgui.WidgetList

object Dashboard {
  case class Props(proxy: ModelProxy[(Map[UUID, OpenWidget], FrontEndState)])

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props) =
      <.div(
        ReactGridLayout(
          width = 1920,
          cols = 8,
          draggableHandle = "." + DashboardCSS.widgetPanelHeader.htmlClass,
          onLayoutChange = layout => {
            layout.asInstanceOf[LayoutData].foreach(
              g => {
                p.proxy()._1.values.toList.foreach(widget => if(widget.id.toString == g.i) {
                  val newLayout = WidgetLayout(g.x, g.y, g.w, g.h)
                  SPGUICircuit.dispatch(UpdateLayout(widget.id, newLayout))
                })
              }
            )
          },
          children = for{
            tuple <- p.proxy()._1
            openWidget = tuple._2
            frontEndState = p.proxy()._2
          }
          yield ReactGridLayoutItem(
            key = openWidget.id.toString,
            i = openWidget.id.toString,
            x = openWidget.layout.x,
            y = openWidget.layout.y,
            w = openWidget.layout.w,
            h = openWidget.layout.h,
            isDraggable = true,
            isResizable = true,
            child = DashboardItem(
              WidgetList.map(openWidget.widgetType)(
                SPWidgetBase(
                  openWidget.id,
                  openWidget.data
                )
              ),
              openWidget.widgetType,
              openWidget.id
            )
          )
        )
      )
  }

  private val component = ReactComponentB[Props]("Dashboard")
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[(Map[UUID, OpenWidget], FrontEndState)]) =
    component(Props(proxy))
}
