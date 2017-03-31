package spgui.widgets.sopmaker

import java.util.UUID
import japgolly.scalajs.react._

import japgolly.scalajs.react.vdom.all.{ a, h1, h2, href, div, className, onClick, br, key }
import japgolly.scalajs.react.vdom.svg.all._
import paths.mid.Bezier
import paths.mid.Rectangle


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
  case class State(sop: List[String])

  private class Backend($: BackendScope[Unit, State]) {
    val eventHandler = BackendCommunication.getMessageObserver(
      mess => {
        println("[SopMaker] Got event " + mess.toString)
      },
      "events"
    )

    val points: List[(Double,Double)] = List(
      (0, 50),
      (50, 70),
      (100, 40),
      (150, 30),
      (200, 60),
      (250, 80),
      (300, 50)
    )

    def render(s: State) = {
      val line = Bezier(points)
      val circles = line.path.points.map(p => circle(
        r := 5,
        cx := p(0),
        cy := p(1),
        stroke := "red",
        strokeWidth := 2,
        fill := "white",
        key := UUID.randomUUID().toString // reacts wants something unique...
      ))
      val rr = Rectangle(top = 0,left = 0,bottom = 200,right = 200)

      div(
        h2("Insert sop here:"),
        br(),
        svg(width := 400, height := 400,
          g(transform := "translate(100, 0)",
            path(d := rr.path.print, stroke := "blue", fill := "white"),
            text(transform := "translate(0,15)", "Testing"),
            path(d := line.path.print, stroke := "red", fill := "none"),
            circles

// rect(0, 0, struct.clientSideAdditions.width, struct.clientSideAdditions.height, 5).attr({fill:bgcolor, 'stroke-width':1, text: struct.clientSideAdditions.drawnText})
          )
        )
      )
    }

    def onUnmount() = {
      println("Unmounting sopmaker")
      eventHandler.kill()
      Callback.empty
    }

  }

  private val component = ReactComponentB[Unit]("SopMakerWidget")
    .initialState(State(sop = List()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}
