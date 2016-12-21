package sp.core

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.Dynamic._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.ReactAttr

object Grid {
  val component = ReactComponentB[Unit]("Grid")
    .renderBackend[Backend]
    .build
  
  class Backend($: BackendScope[Unit, Unit]) {
    val klayout: js.UndefOr[js.Array[js.Object with js.Dynamic]] =
      Some(js.Array(
        js.Dynamic.literal(i = "a", x = 1, y = 2, w = 3, h = 1),
        js.Dynamic.literal(i = "b", x = 2, y = 4, w = 1, h = 2)
      )).orUndefined
    
    def render() =
      <.div("hello from MyGrid",
        ^.backgroundColor := "#ff000f",
        ReactGridLayoutWrapper(
          width = 1920,
          layout = klayout,
          cols = 10,
          onLayoutChange = (layout:js.Object) => println(layout.toString())
        ).apply(
          <.div("a",
            ^.key := "a",
            ^.backgroundColor := "#00ffff"
          ),
          <.div("b",
            ^.key := "b",
            ^.backgroundColor := "#00ffff"
          ),
          <.div(
            "c",
            ^.key := "c",
            ^.backgroundColor := "#00ffff",
            ReactAttr.Generic("data-grid") := LayoutItem(
              i ="c", x = 3, y = 4, w = 5, h = 1
            ).toJS
          )
        )
      )
  }

  case class LayoutItem(
    i: String,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    minW: js.UndefOr[Int] = 0,
    maxW: js.UndefOr[Int] = Integer.MAX_VALUE,
    minH: js.UndefOr[Int] = 0,
    maxH: js.UndefOr[Int] = Integer.MAX_VALUE,
    static: js.UndefOr[Boolean] = false,
    isDraggable: js.UndefOr[Boolean] = true,
    isResizable: js.UndefOr[Boolean] = true
  ) {

    def toJS: js.Object = {
      val p = js.Dynamic.literal()
      p.updateDynamic("i")(i)
      p.updateDynamic("x")(x)
      p.updateDynamic("y")(y)
      p.updateDynamic("w")(w)
      p.updateDynamic("h")(h)
      minW.foreach(v => p.updateDynamic("minW")(v))
      maxW.foreach(v => p.updateDynamic("maxW")(v))
      minH.foreach(v => p.updateDynamic("minH")(v))
      maxH.foreach(v => p.updateDynamic("maxH")(v))
      static.foreach(v => p.updateDynamic("static")(v))
      isDraggable.foreach(v => p.updateDynamic("isDraggable")(v))
      isResizable.foreach(v => p.updateDynamic("isResizable")(v))

      p
    }
  }

  @js.native
  @JSName("ReactGridLayout")
  object ReactGridLayout extends js.Object {}

  case class ReactGridLayoutWrapper(
    width: Int,
    autoSize: js.UndefOr[Boolean] = js.undefined,
    cols: js.UndefOr[Int] = js.undefined,
    draggableCancel: js.UndefOr[String] = js.undefined,
    draggableHandle: js.UndefOr[String] = js.undefined,
    verticalCompact: js.UndefOr[Boolean] = js.undefined,
    layout: js.UndefOr[js.Array[js.Object with js.Dynamic]] = js.undefined,
    marginX: js.UndefOr[Int] = js.undefined,
    marginY: js.UndefOr[Int] = js.undefined,
    containerPaddingX: js.UndefOr[Int] = js.undefined,
    containerPaddingY: js.UndefOr[Int] = js.undefined,
    isDraggable: js.UndefOr[Boolean] = js.undefined,
    isResizable: js.UndefOr[Boolean] = js.undefined,
    useCSSTransforms: js.UndefOr[Boolean] = js.undefined,
    onLayoutChange: (js.Array[js.Object with js.Dynamic]) => Unit
  ) {
    def toJS: js.Object = {global
      val p = js.Dynamic.literal()
      p.updateDynamic("width")(width)
      autoSize.foreach(v => p.updateDynamic("autoSize")(v))
      cols.foreach(v => p.updateDynamic("cols")(v))
      draggableCancel.foreach(v => p.updateDynamic("draggableCancel")(v))
      draggableHandle.foreach(v => p.updateDynamic("draggableHandle")(v))
      verticalCompact.foreach(v => p.updateDynamic("verticalCompact")(v))
      layout.foreach(v => p.updateDynamic("layout")(v))
      marginX.foreach(v => p.updateDynamic("marginX")(v))
      marginY.foreach(v => p.updateDynamic("marginY")(v))
      containerPaddingX.foreach(v => p.updateDynamic("containerPaddingX")(v))
      containerPaddingY.foreach(v => p.updateDynamic("containerPaddingY")(v))
      isDraggable.foreach(v => p.updateDynamic("isDraggable")(v))
      isResizable.foreach(v => p.updateDynamic("isResizable")(v))
      useCSSTransforms.foreach(v => p.updateDynamic("useCSSTransforms")(v))
      p.updateDynamic("onLayoutChange")(onLayoutChange)

      p
    }

    def apply(children : ReactNode*) = {
      // access real js component
      val f = React.asInstanceOf[js.Dynamic].createFactory(ReactGridLayout)
      f(toJS, children.toJsArray).asInstanceOf[ReactComponentU_]
    }
  }
}


/*
 this is the full list of grid props

 width: number,
 autoSize: ?boolean = true,
 cols: ?number = 12,
 draggableCancel: ?string = '',
 draggableHandle: ?string = '',
 verticalCompact: ?boolean = true,
 layout: ?array = null, // If not provided, use data-grid props on children
 margin: ?[number, number] = [10, 10],
 containerPadding: ?[number, number] = margin,
 rowHeight: ?number = 150,
 isDraggable: ?boolean = true,
 isResizable: ?boolean = true,
 useCSSTransforms: ?boolean = true,
 onLayoutChange: (layout: Layout) => void,
 
 // TODO: everything below this point. not sure how
 // or maybe not. we might not even need it. 

type ItemCallback = (layout: Layout, oldItem: LayoutItem, newItem: LayoutItem,
 placeholder: LayoutItem, e: MouseEvent, element: HTMLElement) => void;

 onDragStart: ItemCallback,
 onDrag: ItemCallback,
 onDragStop: ItemCallback,
 onResizeStart: ItemCallback,
 onResize: ItemCallback,
 onResizeStop: ItemCallback
*/


