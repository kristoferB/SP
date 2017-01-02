package spgui

import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.Dynamic._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._

object CSS extends StyleSheet.Inline {
  import dsl._
  val widget = style(
    backgroundColor.orange,
    transition := "all 0.4s linear"
  )
  val reactDraggableDragging = style(
    backgroundColor.royalblue
  )
  val dashboard = style(
    backgroundColor.red
  )
}

object Grid {
  CSS.addToDocument()

  val component = ReactComponentB[Unit]("Grid")
    .render(_ =>
    <.div(
      ^.className:=CSS.dashboard.htmlClass,
      ReactGridLayout(
        width = 1920,
        onLayoutChange = (layout:js.Object) => println(layout.toString()),
        ReactGridLayoutItem(key = "c", i = "c", x = 3, y = 4, w = 5, h = 1, isDraggable = true, child = <.h3("c")),
        ReactGridLayoutItem(key = "d", i = "c", x = 0, y = 0, w = 1, h = 1, isDraggable = false, child = <.h3("C: undraggable"))
      )
    )
  )
    .build
}

object ReactGridLayoutItem {
  def apply(key: String,
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
    isResizable: js.UndefOr[Boolean] = true,
    child: ReactNode
  ) = {
    def jsObject: js.Object = {
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
    <.div(
      ^.key := key,
      ^.className:=CSS.widget.htmlClass,
      ReactAttr.Generic("data-grid") := jsObject,
      child
    )
  }
}

@js.native
@JSName("ReactGridLayout")
object ReactGridLayoutJS extends js.Object {}

object ReactGridLayout {

  case class Props(
    width: Int,
    autoSize: js.UndefOr[Boolean] = true,
    cols: js.UndefOr[Int] = 12,
    draggableCancel: js.UndefOr[String] = "",
    draggableHandle: js.UndefOr[String] = "",
    verticalCompact: js.UndefOr[Boolean] = true,
    layout: js.UndefOr[js.Array[js.Object with js.Dynamic]] = js.undefined,
    margin: js.UndefOr[js.Array[Int]] = js.Array(10, 10),
    containerPadding: js.UndefOr[js.Array[Int]] = js.Array(10,10),
    rowHeight: js.UndefOr[Int] = 150,
    isDraggable: js.UndefOr[Boolean] = true,
    isResizable: js.UndefOr[Boolean] = true,
    useCSSTransforms: js.UndefOr[Boolean] = true,
    onLayoutChange: (js.Array[js.Object with js.Dynamic]) => Unit
  )

  case class ReactGridLayoutFacade(props: Props) {
    def toJS: js.Object = {global
      val p = js.Dynamic.literal()
      p.updateDynamic("width")(props.width)
      props.autoSize.foreach(v => p.updateDynamic("autoSize")(v))
      props.cols.foreach(v => p.updateDynamic("cols")(v))
      props.draggableCancel.foreach(v => p.updateDynamic("draggableCancel")(v))
      props.draggableHandle.foreach(v => p.updateDynamic("draggableHandle")(v))
      props.verticalCompact.foreach(v => p.updateDynamic("verticalCompact")(v))
      props.layout.foreach(v => p.updateDynamic("layout")(v))
      props.margin.foreach(v => p.updateDynamic("margin")(v))
      props.containerPadding.foreach(v => p.updateDynamic("containerPadding")(v))
      props.rowHeight.foreach(v => p.updateDynamic("rowHeight")(v))
      props.isDraggable.foreach(v => p.updateDynamic("isDraggable")(v))
      props.isResizable.foreach(v => p.updateDynamic("isResizable")(v))
      props.useCSSTransforms.foreach(v => p.updateDynamic("useCSSTransforms")(v))
      p.updateDynamic("onLayoutChange")(props.onLayoutChange)

      p
    }
  }

  def apply(width: Int, onLayoutChange: (js.Array[js.Object with js.Dynamic]) => Unit, children: ReactNode*) = {
    // access real js component
    val f = React.asInstanceOf[js.Dynamic].createFactory(ReactGridLayoutJS)
    val facade = ReactGridLayoutFacade(Props(width = width, onLayoutChange = onLayoutChange))
    f(facade.toJS, children.toJsArray).asInstanceOf[ReactComponentU_]
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
