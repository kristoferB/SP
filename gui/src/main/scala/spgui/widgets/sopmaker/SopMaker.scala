package spgui.widgets.sopmaker

import java.util.UUID
import japgolly.scalajs.react._

//import japgolly.scalajs.react.vdom.all.{ a, h1, h2, href, div, className, onClick, br, key }
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.svg
//import paths.mid.Bezier
//import paths.mid.Rectangle


import spgui.components.DragAndDrop.{ OnDragMod, OnDropMod, DataOnDrag, OnDataDrop }

import spgui.communication._
import sp.domain._
import sp.messages._
import sp.messages.Pickles._

object measures {
  // from sopdrawer.js
  val margin = 15
  val opH = 50
  val opW = 60
  val para = 7
  val arrow = 5
  val textScale = 6
  val animTime = 0
  val commonLineColor = "black"
  val condLineHeight = 12
  val nameLineHeight = 50
}

object SopMakerWidget {
  case class State(drag: String, drop: String, sop: List[String])

  private class Backend($: BackendScope[Unit, State]) {
    /*
    val eventHandler = BackendCommunication.getMessageObserver(
      mess => {
        println("[SopMaker] Got event " + mess.toString)
      },
      "events"
    )
     */

    def handleDrag(drag: String)(e: ReactDragEventFromInput): Callback = {
      Callback({
        e.dataTransfer.setData("json", drag)
      })
    }

    def handleDrop(drop: String)(e: ReactDragEvent): Callback = {
      val drag = e.dataTransfer.getData("json")
      Callback.log("Dropping " + drag + " onto " + drop)
    }

    def render(state: State) = {
      val ops = List(Operation("op1"), Operation("op2"))
      val idm = ops.map(o=>o.id -> o).toMap

      //ops

      val fakeSop = SOP(ops(0), EmptySOP)
      def drawSop(s: SOP): Seq[VdomTag] = {
        s match {
          case s:Sequence =>
            s.sop.flatMap(drawSop)
          case h:Hierarchy =>
            val opname = idm.get(h.operation).map(_.name).getOrElse("[unknown op]")
            Seq(
              <.div(OnDragMod(handleDrag(opname)), OnDropMod(handleDrop(opname)),
                svg.svg(svg.width := measures.opW, svg.height := measures.opH,
                  svg.rect(svg.x:=0, svg.y:=0, svg.width:=measures.opW, svg.height:=measures.opH, svg.rx:=5, svg.ry:=5, svg.fill := "white", svg.stroke:="black", svg.strokeWidth:=1),
                  svg.text(svg.x:="50%", svg.y:="50%", svg.textAnchor:="middle", svg.dy:=".3em", opname)
                ))
            )
        }
      }

      <.div(
        <.h2("Insert sop here:"),
        OnDataDrop(string => Callback.log("Received data: " + string)),
        <.br(),
        drawSop(fakeSop).toTagMod
      )
    }

    def onUnmount() = Callback {
      println("Unmounting sopmaker")
    }

  }

  private val component = ScalaComponent.builder[Unit]("SopMakerWidget")
    .initialState(State(drag = "", drop = "", sop = List()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}
