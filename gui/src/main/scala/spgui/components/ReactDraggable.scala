package spgui.components

import japgolly.scalajs.react._
import scala.scalajs.js
import js.annotation.{ JSName, JSGlobal }
import japgolly.scalajs.react.vdom.html_<^.VdomArray
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html._
import org.scalajs.dom.console

object ReactDraggable {
  @js.native
  @JSGlobal("ReactDraggable")
  object ReactDraggableJS extends js.Object

  type DraggableEventHandler = (ReactEvent, DraggableDataJS) => Unit
  type DraggableEventHandlerJS = js.Function2[ReactEvent, DraggableDataJS, Unit]

  trait DraggableData extends js.Object {
    var node: org.scalajs.dom.html.Element = js.native
    var x: Float= js.native
    var y: Float= js.native
    var deltaX: Float= js.native
    var deltaY: Float= js.native
    var lastX: Float= js.native
    var lastY: Float= js.native
  }
    
  type DraggableDataJS = js.Object

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
    var onMouseDown: js.Function1[ReactMouseEvent, Unit] = js.native
    var onStart: DraggableEventHandlerJS = js.native
    var onDrag: DraggableEventHandlerJS = js.native
    var onStop: DraggableEventHandlerJS = js.native
    var position: Position = js.native
  }

  // trait Bounds extends js.Object {
  //   var left: Float = js.native
  //   var top: Float = js.native
  //   var right: Float = js.native
  //   var bottom: Float  = js.native
  // }
  type Bounds = String

  trait Position extends js.Object {
    var x: Float = js.native
    var y: Float = js.native
  }

  def apply(
    allowAnyClick: Boolean = true,
    axis: String = "both",
    bounds: String = "",
    cancel: String = "",
    defaultClassName: String = "",
    defaultClassNameDragging: String = "",
    defaultClassNameDragged: String = "",
    defaultPosition: (Float, Float) = (0f, 0f),
    disabled: Boolean = false,
    grid: (Float, Float)= (1f, 1f),
    handle: String = "",
    offsetParent: org.scalajs.dom.html.Element = null,
    onMouseDown: ((ReactMouseEvent) => Unit) = ((e: ReactMouseEvent) => Unit),

    onStart: DraggableEventHandler =
      (e: ReactEvent,d: DraggableDataJS) => console.log(d.asInstanceOf[DraggableData]),
    onDrag: DraggableEventHandler =
      (e: ReactEvent, d: DraggableDataJS) => console.log(d.asInstanceOf[DraggableData]),
    onStop: DraggableEventHandler =
      (e: ReactEvent, d: DraggableDataJS) => console.log(d.asInstanceOf[DraggableData]),

    position: (Float, Float) = (0f, 0f)
  )(children: VdomNode ) = {
    val p = (new js.Object).asInstanceOf[Props]
    p.allowAnyClick = allowAnyClick
    p.axis = axis

    // var b = (new js.Object).asInstanceOf[Bounds]
    // b.left = bounds._1
    // b.top = bounds._2
    // b.right = bounds._3
    // b.bottom = bounds._4
    p.bounds = bounds

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

    
    val component = JsComponent[Props, Children.Varargs, Null](ReactDraggableJS)
    
    component(p)(children)
  }
}
