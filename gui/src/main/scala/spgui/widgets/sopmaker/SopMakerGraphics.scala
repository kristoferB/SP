package spgui.widgets.sopmaker

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.svg

object SopMakerGraphics {

  trait Rect extends js.Object {
    var left: Float = js.native
    var top: Float = js.native
    var width: Float = js.native
    var height: Float = js.native
  }

  def sop(label: String, x: Int, y: Int) =
    <.span(
      ^.className := SopMakerCSS.sopComponent.htmlClass,
      ^.style := {
        var rect =  (js.Object()).asInstanceOf[Rect]
        rect.left = x
        rect.top = y
        rect.height = SopMakerWidget.opHeight
        rect.width = SopMakerWidget.opWidth
        rect
      },
      svg.svg(
        svg.width := SopMakerWidget.opWidth.toInt,
        svg.height:= SopMakerWidget.opHeight.toInt,
        svg.svg(
          svg.width := SopMakerWidget.opWidth.toInt,
          svg.height:= SopMakerWidget.opHeight.toInt,
          svg.x := 0,
          svg.y := 0,
          svg.rect(
            svg.x := 0,
            svg.y := 0,
            svg.width := SopMakerWidget.opWidth.toInt,
            svg.height:= SopMakerWidget.opHeight.toInt,
            svg.rx := 6, svg.ry := 6,
            svg.fill := "white",
            svg.stroke := "black",
            svg.strokeWidth := 1
          ),
          svg.svg(    
            svg.text(
              svg.x := "50%",
              svg.y := "50%",
              svg.textAnchor := "middle",
              svg.dy := ".3em",
              label
            )
          )
        )
      )
    )



  def parallelBars(x: Float, y: Float, w:Float): TagMod =
    <.span(
      ^.className := SopMakerCSS.sopComponent.htmlClass,
      ^.style := {
        var rect =  (js.Object()).asInstanceOf[Rect]
        rect.left = x + SopMakerWidget.opWidth/2
        rect.top = y
        rect.height = 12
        rect.width = w
        rect
      },
      svg.svg(
        svg.width := "100%",
        svg.height := "100%",
        svg.svg(
          svg.width := w.toInt,
          svg.height := 12,
          svg.rect(
            svg.x := 0,
            svg.y := 0,
            svg.width:=w.toInt,
            svg.height:=4,
            svg.fill := "black",
            svg.strokeWidth:=1
          ),
          svg.rect(
            svg.x := 0,
            svg.y :=  8,
            svg.width:=w.toInt,
            svg.height:=4,
            svg.fill := "black",
            svg.strokeWidth:=1
          )
        )
      )
    )
}
