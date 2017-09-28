package spgui.dashboard

import japgolly.scalajs.react._
import scala.scalajs.js
import js.annotation.{ JSName, JSGlobal }
import japgolly.scalajs.react.vdom.html_<^.VdomArray
import java.util.UUID

object ReactGridLayout {

  @js.native
  @JSGlobal("ReactGridLayout")
  object ReactGridLayoutJS extends js.Object

  @js.native
  trait LayoutElement extends js.Object {
    var i: String = js.native
    var x: Int = js.native
    var y: Int = js.native
    var w: Int = js.native
    var h: Int = js.native
    var minW: Int = js.native
    var maxW: Int = js.native
    var minH: Int = js.native
    var maxH: Int = js.native
    var static: Boolean = js.native
    var isDraggable: Boolean = js.native
    var isResizable: Boolean = js.native
  }
  // atm there is no perfect way to facade "options" kind of jsObjects, but this is one way
  object LayoutElement {
    def apply(
      i: String,
      x: Int,
      y: Int,
      w: Int,
      h: Int,
      minW: Int = 2,
      maxW: Int = Integer.MAX_VALUE,
      minH: Int = 1,
      maxH: Int = Integer.MAX_VALUE,
      static: Boolean = false,
      isDraggable: Boolean = true,
      isResizable: Boolean = true
    ): LayoutElement = {
      val jsObj = (new js.Object).asInstanceOf[LayoutElement]
      jsObj.i = i
      jsObj.x = x
      jsObj.y = y
      jsObj.w = w
      jsObj.h = h
      jsObj.minW = minW
      jsObj.maxW = maxW
      jsObj.minH = minH
      jsObj.maxH = maxH
      jsObj.static = static
      jsObj.isDraggable = isDraggable
      jsObj.isResizable = isResizable
      jsObj
    }
  }
  type Layout = js.Array[LayoutElement]

  @js.native
  trait Props extends js.Object {
    var width: Int = js.native
    var autoSize: Boolean = js.native
    var cols: Int = js.native
    var draggableCancel: String = js.native
    var draggableHandle: String = js.native
    var verticalCompact: Boolean = js.native
    var layout: Layout = js.native
    var margin: js.Array[Int] = js.native
    var containerPadding: js.Array[Int] = js.native
    var rowHeight: Int = js.native
    var isDraggable: Boolean = js.native
    var isResizable: Boolean = js.native
    var useCSSTransforms: Boolean = js.native
    var onLayoutChange: js.Function1[Layout, Unit] = js.native
    var onResize: js.Function1[Layout, Unit] = js.native
    var onResizeStart: js.Function1[Layout, Unit] = js.native
  }

  def apply(
    width: Int,
    autoSize: Boolean = true,
    cols: Int = 12,
    draggableCancel: String = "",
    draggableHandle: String = "",
    verticalCompact: Boolean = true,
    layout: js.Array[LayoutElement] = js.Array(),
    margin: js.Array[Int] = js.Array(3,3),
    containerPadding: js.Array[Int] = js.Array(3,3),
    rowHeight: Int = 50,
    isDraggable: Boolean = true,
    isResizable: Boolean = true,
    useCSSTransforms: Boolean = true,
    onLayoutChange: Layout => Unit = x => Unit,
    onResize: Layout => Unit = x => Unit,
    onResizeStart: Layout => Unit = x => Unit,
    children: VdomArray
  ) = {
    val p = (new js.Object).asInstanceOf[Props]
    p.width = width
    p.autoSize = autoSize
    p.cols = cols
    p.draggableCancel = draggableCancel
    p.draggableHandle = draggableHandle
    p.verticalCompact = verticalCompact
    p.layout = layout
    p.margin = margin
    p.containerPadding = containerPadding
    p.rowHeight = rowHeight
    p.isDraggable = isDraggable
    p.isResizable = isResizable
    p.useCSSTransforms = useCSSTransforms
    p.onLayoutChange = onLayoutChange
    p.onResize = onResize
    p.onResizeStart = onResizeStart

    val component = JsComponent[Props, Children.Varargs, Null](ReactGridLayoutJS)
    component(p)(children)
  }
}
