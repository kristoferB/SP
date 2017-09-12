package spgui.dragging

import java.util.UUID
import japgolly.scalajs.react._

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.svg

import spgui.communication._
import sp.domain._
import sp.messages._
import sp.messages.Pickles._
import scalacss.ScalaCssReact._
import spgui.components.ReactDraggable
import scala.scalajs.js

import org.scalajs.dom.window
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.document

import diode.react.ModelProxy
import spgui.circuit._
import spgui.circuit.{SetDraggableData, SetDraggableRenderStyle}


object Dragging {
  case class Props(proxy: ModelProxy[DraggingState])

  case class State(x: Float = 0f, y: Float = 0f)

  case class Data(
    label: String,
    id: UUID,
    typ: String
  )

  trait Rect extends js.Object {
    var left: Float = js.native
    var top: Float = js.native
    var width: Float = js.native
    var height: Float = js.native
  }

  val opHeight = 80f
  val opWidth = 80f

  var updateMouse = (x:Float, y:Float) => {}

  window.onmouseup = ((e:MouseEvent) => onDragStop())

  class Backend($: BackendScope[Props, State]) {
    updateMouse = (x,y) => $.setState(State(x,y)).runNow()

    def render(state: State, props: Props) = {
      <.span(
        ^.pointerEvents.none,
        {if(!props.proxy().dragging) ^.className := DraggingCSS.hidden.htmlClass
        else EmptyVdom},
        props.proxy().renderStyle match {
          case _ =>
            <.span(
//              ^.pointerEvents.none,
              ^.className := DraggingCSS.dragElement.htmlClass,
              ^.style := {
                var rect =  (js.Object()).asInstanceOf[Rect]
                rect.left = state.x - opWidth/2
                rect.top = state.y - opHeight/2
                rect.height = opHeight
                rect.width = opWidth
                rect
              },
              svg.svg(
                svg.svg(
                  svg.width := opWidth.toInt,
                  svg.height:= opHeight.toInt,
                  svg.x := 0,//props.proxy().x.toInt,
                  svg.y := 0,//props.proxy().y.toInt,
                  svg.rect(
                    svg.x := 0,
                    svg.y := 0,
                    svg.width := opWidth.toInt,
                    svg.height:= opHeight.toInt,
                    svg.rx := 6, svg.ry := 6,
                    svg.fill := "white",
                    svg.stroke := "black",
                    svg.strokeWidth := 1
                  ),
                  svg.svg(
                    //SopMakerCSS.opText,
                    svg.text(
                      svg.x := "50%",
                      svg.y := "50%",
                      svg.textAnchor := "middle",
                      svg.dy := ".3em", props.proxy().data
                    )
                  )
                )
              )
            )
        }
      )
    }
  }

  private var myself = null

  private val component = ScalaComponent.builder[Props]("Dragging")
    .initialState(State())
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[DraggingState]) = component(Props(proxy))

  def onDragStart(data: String, x:Float, y:Float) = {
    SPGUICircuit.dispatch(SetDraggableData(data))
    SPGUICircuit.dispatch(SetCurrentlyDragging(true))
    updateMouse(x,y)
  }

  def onDragMove(x:Float, y:Float) = {
    updateMouse(x,y)
  }


  def onDragStop() = {
    SPGUICircuit.dispatch(SetCurrentlyDragging(false))
  }


  def setDraggingStyle(style: String) = {
    SPGUICircuit.dispatch(SetDraggableRenderStyle(style))
  }

  def mouseMoveCapture = Seq(
    ^.onTouchMoveCapture ==> {
      (e: ReactTouchEvent) => Callback ({
        var x = 0f; var y = 0f
        for(n <- 0 to e.touches.length-1) {
          x += e.touches.item(n).pageX.toFloat
          y += e.touches.item(n).pageY.toFloat
        }
        Dragging.onDragMove(x, y)
      })
    },
    ^.onMouseMove ==> {
      (e:ReactMouseEvent) => Callback{
        Dragging.onDragMove(e.pageX.toFloat, e.pageY.toFloat)
      }
    }
  ).toTagMod
}
