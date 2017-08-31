package spgui.dashboard

import japgolly.scalajs.react._
import scala.scalajs.js
import js.annotation.JSName
import js.Dynamic._
import js.JSConverters._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.DevDefaults._

import spgui.SPWidget
import spgui.dashboard.{ ResponsiveReactGridLayout => RRGL }

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

object GridTest {
  def apply() = SPWidget(spwb => Grid.component())
}

object Grid {
  CSS.addToDocument()

  val component = ScalaComponent.builder[Unit]("Grid")
    .render(_ =>
    <.div(
      ^.className:=CSS.dashboard.htmlClass,
      RRGL(
        width = 1920,
        draggableHandle = "",
        onLayoutChange = (layout:js.Object) => Unit,
        children = VdomArray(
          <.div(
            <.h3("c"),
            ^.key := "c",
            ^.className:=CSS.widget.htmlClass,
            VdomAttr("data-grid") := RRGL.LayoutElement(i = "c", x = 3, y = 4, w = 5, h = 1, isDraggable = true)
          ),
          <.div(
            <.h3("C: undraggable"),
            ^.key := "d",
            ^.className:=CSS.widget.htmlClass,
            VdomAttr("data-grid") := RRGL.LayoutElement(i = "c", x = 0, y = 0, w = 1, h = 1, isDraggable = false)
          )
        )
      )
    )
  )
    .build
}
