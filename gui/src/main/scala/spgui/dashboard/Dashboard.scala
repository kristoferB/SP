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

import spgui.dashboard.{ ReactGridLayout => RGL }

import scala.scalajs.js
import org.scalajs.dom.console
import js.JSConverters._

import scala.util.Random

import org.scalajs.dom.window

object Dashboard {
  case class Props(proxy: ModelProxy[(Map[UUID, OpenWidget], GlobalState)])
  case class State(width: Int)

  class Backend($: BackendScope[Props, State]) {
    def render(p: Props, s: State) = {
      window.onresize = { e: org.scalajs.dom.Event =>
      println(window.innerWidth)
      $.setState(State(
        window.innerWidth.toInt)).runNow()
    }

      val widgets = for {
        openWidget <- p.proxy()._1.values
      } yield {
        val frontEndState = p.proxy()._2

        <.div(
          DashboardItem(
            WidgetList.map(openWidget.widgetType)._1(
              SPWidgetBase(openWidget.id, frontEndState)
            ),
            openWidget.widgetType,
            openWidget.id
          ),
          ^.key := openWidget.id.toString
        )
      }

      val bigLayout = for {
        openWidget <- p.proxy()._1.values
      } yield {
          RGL.LayoutElement(
            i = openWidget.id.toString,
            x = openWidget.layout.x,
            y = openWidget.layout.y,
            w = openWidget.layout.w,
            h = openWidget.layout.h,
            isDraggable = true,
            isResizable = true
          )
      }
      console.log(bigLayout.toJSArray.asInstanceOf[RGL.Layout])
      val rg = RGL(
        layout = bigLayout.toJSArray.asInstanceOf[RGL.Layout],
        width = s.width,
        draggableHandle = "." + DashboardCSS.widgetPanelHeader.htmlClass,
        onLayoutChange = layout => {
          layout.asInstanceOf[RGL.Layout].foreach(
            g => {
              p.proxy()._1.values.toList.foreach(widget => if (widget.id.toString == g.i) {
                val newLayout = WidgetLayout(g.x, g.y, g.w, g.h, widget.layout.collapsedHeight)
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
    .initialState(State( window.innerWidth.toInt))
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[(Map[UUID, OpenWidget], GlobalState)]) =
    component(Props(proxy))
}
