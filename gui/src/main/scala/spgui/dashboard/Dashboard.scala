package spgui.dashboard

import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import diode.react.ModelProxy

import scalajs.js.Dynamic
import scalajs.js.JSON
import spgui.SPWidgetBase
import spgui.circuit._
import spgui.WidgetList

import spgui.dashboard.{ ResponsiveReactGridLayout => RRGL }

object Dashboard {
  case class Props(proxy: ModelProxy[(Map[UUID, OpenWidget], GlobalState)])

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props) = {

      val widgets = for {
        openWidget <- p.proxy()._1.values if WidgetList.map.contains(openWidget.widgetType)
      } yield {

        val frontEndState = p.proxy()._2

        val layoutElement = RRGL.LayoutElement(
          i = openWidget.id.toString,
          x = openWidget.layout.x,
          y = openWidget.layout.y,
          w = openWidget.layout.w,
          h = openWidget.layout.h,
          isDraggable = true,
          isResizable = true
        )

        <.div(
          DashboardItem(
            WidgetList.map(openWidget.widgetType)._1(
              SPWidgetBase(openWidget.id, frontEndState)
            ),
            openWidget.widgetType,
            openWidget.id
          ),
          ^.key := openWidget.id.toString,
          VdomAttr("data-grid") := layoutElement
        )
      }

      val rg = RRGL(
        width = 1920,
        draggableHandle = "." + DashboardCSS.widgetPanelHeader.htmlClass,
        onLayoutChange = layout => {
          layout.asInstanceOf[RRGL.Layout].foreach(
            g => {
              p.proxy()._1.values.toList.foreach(widget => if (widget.id.toString == g.i) {
                val newLayout = WidgetLayout(g.x, g.y, g.w, g.h)
                SPGUICircuit.dispatch(UpdateLayout(widget.id, newLayout))
              })
            }
          )
        },
        children = widgets.toVdomArray
      )

      <.div(
        rg
      )
    }
  }

  private val component = ScalaComponent.builder[Props]("Dashboard")
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[(Map[UUID, OpenWidget], GlobalState)]) =
    component(Props(proxy))
}
