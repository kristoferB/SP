package spgui.widgets.sopmaker

import java.util.UUID
import japgolly.scalajs.react._

import japgolly.scalajs.react.vdom.all.{ a, h1, h2, href, div, className, onClick, br, key }
import japgolly.scalajs.react.vdom.svg.all._
import paths.mid.Bezier
import paths.mid.Rectangle


import spgui.components.DragAndDrop.{ OnDragMod, OnDropMod }

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
    val eventHandler = BackendCommunication.getMessageObserver(
      mess => {
        println("[SopMaker] Got event " + mess.toString)
      },
      "events"
    )

    def handleDrag(drag: String)(e: ReactDragEventFromInput): Callback = {
      Callback({
        e.dataTransfer.setData("json", drag)
      })
    }

    def handleDrop(drop: String)(e: ReactDragEvent): Callback = {
      val drag = e.dataTransfer.getData("json")
      Callback({
        println("Dropping " + drag + " onto " + drop)
      })
    }

    def render(state: State) = {
      val ops = List(Operation("op1"), Operation("op2"))
      val idm = ops.map(o=>o.id -> o).toMap

      val fakeSop = Sequence(ops(0), ops(1))
      def drawSop(s: SOP): Seq[ReactTag] = {
        s match {
          case s:Sequence =>
            s.sop.flatMap(drawSop)
          case h:Hierarchy =>
            val opname = idm.get(h.operation).map(_.name).getOrElse("[unknown op]")
            // val preGuardYPos = measures.condLineHeight
            // val preActionYPos = preGuardYPos + struct.clientSideAdditions.preGuards.length * measures.condLineHeight
            // val preLineYPos = preActionYPos + struct.clientSideAdditions.preActions.length * measures.condLineHeight
            // val postGuardYPos = preLineYPos + measures.nameLineHeight
            // val postLineYPos = postGuardYPos - measures.condLineHeight
            // val postActionYPos = postGuardYPos + struct.clientSideAdditions.postGuards.length * measures.condLineHeight

            Seq(
              div(OnDragMod(handleDrag(opname)), OnDropMod(handleDrop(opname)),
                svg(width := measures.opW, height := measures.opH,
                  rect(x:=0, y:=0, width:=measures.opW, height:=measures.opH, rx:=5, ry:=5, fill := "white", stroke:="black", strokeWidth:=1),
                  text(x:="50%", y:="50%", textAnchor:="middle", dy:=".3em", opname) // ,
                  // text(struct.clientSideAdditions.width / 2, ystart + measures.condLineHeight*j, array[j])
                ))
            )
        }
      }

      div(
        h2("Insert sop here:"),
        br(),
        drawSop(fakeSop)
      )
    }

    def onUnmount() = {
      println("Unmounting sopmaker")
      eventHandler.kill()
      Callback.empty
    }

  }

  private val component = ScalaComponent.builder[Unit]("SopMakerWidget")
    .initialState(State(drag = "", drop = "", sop = List()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}
