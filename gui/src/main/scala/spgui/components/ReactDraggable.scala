package spgui.components

import japgolly.scalajs.react._
import scala.scalajs.js
import js.annotation.{ JSName, JSGlobal }
import japgolly.scalajs.react.vdom.html_<^.VdomArray
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html._

object ReactDraggable {
  @js.native
  @JSGlobal("ReactDraggable")
  object ReactDraggableJS extends js.Object

  type DraggableEventHandler = (ReactEvent, DraggableData) => scala.Option[Boolean]

  case class DraggableData (
    node: org.scalajs.dom.html.Element,
    x: Float, y: Float,
    deltaX: Float, deltaY: Float,
    lastX: Float, lastY: Float
  )

  @js.native
  trait Props extends js.Object {
    var allowAnyClick: Boolean = js.native
    var axis: String = js.native
    var bounds: Bounds = js.native
    var cancel: String = js.native
    var defaultClassName: String = js.native
    var defaultClassNameDragging: String = js.native
    var defaultClassNameDragged: String = js.native
    var defaultPosition: Position = js.native
    var disabled: Boolean = js.native
    var grid: js.Array[Float]= js.native
    var handle: String = js.native
    var offsetParent: org.scalajs.dom.html.Element = js.native
    var onMouseDown: ((ReactMouseEvent) => Unit) = js.native
    var onStart: DraggableEventHandler = js.native
    var onDrag: DraggableEventHandler = js.native
    var onStop: DraggableEventHandler = js.native
    var position: Position = js.native
  }

  trait Bounds extends js.Object {
    var left: Float = js.native
    var top: Float = js.native
    var right: Float = js.native
    var bottom: Float  = js.native
  }

  trait Position extends js.Object {
    var x: Float = js.native
    var y: Float = js.native
  }

  def apply(
    allowAnyClick: Boolean = true,
    axis: String = "",
    bounds: (Float, Float, Float, Float) = (0f, 0f, 0f, 0f),
    cancel: String = "",
    defaultClassName: String = "",
    defaultClassNameDragging: String = "",
    defaultClassNameDragged: String = "",
    defaultPosition: (Float, Float) = (0f, 0f),
    disabled: Boolean = false,
    grid: (Float, Float)= (0f, 0f),
    handle: String = "",
    offsetParent: org.scalajs.dom.html.Element = null,
    onMouseDown: ((ReactMouseEvent) => Unit) = ((e: ReactMouseEvent) => Unit),
    onStart: DraggableEventHandler = (ReactEvent, DraggableData) => None,
    onDrag: DraggableEventHandler = (ReactEvent, DraggableData) => None,
    onStop: DraggableEventHandler = (ReactEvent, DraggableData) => None,
    position: (Float, Float) = (0f, 0f)
  ) = {
    val p = (new js.Object).asInstanceOf[Props]
    p.allowAnyClick = allowAnyClick
    p.axis = axis

    var b = (new js.Object).asInstanceOf[Bounds]
    b.left = bounds._1
    b.top = bounds._2
    b.right = bounds._3
    b.bottom = bounds._4
    p.bounds = b

    p.cancel = cancel
    p.defaultClassName = defaultClassName
    p.defaultClassNameDragging = defaultClassNameDragging
    p.defaultClassNameDragged = defaultClassNameDragged

    var d = (new js.Object).asInstanceOf[Position]
    d.x = defaultPosition._1
    d.y = defaultPosition._2
    p.defaultPosition = d

    p.disabled = disabled

    p.grid = js.Array(grid._1, grid._2)

    p.handle = handle
    p.offsetParent = offsetParent
    p.onMouseDown = onMouseDown
    p.onStart = onStart
    p.onDrag =  onDrag 
    p.onStop =  onStop

    var po = (new js.Object).asInstanceOf[Position]
    po.x = position._1
    po.y = position._2
    p.position = po
  }
}
