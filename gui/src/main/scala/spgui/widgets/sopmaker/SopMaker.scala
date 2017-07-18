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
import scalacss.ScalaCssReact._

trait RenderNode {
  val w: Int
  val h: Int
}

trait RenderGroup extends RenderNode {
  val children: List[RenderNode]
}

case class RenderParallel(w: Int, h:Int, children: List[RenderNode]) extends RenderGroup
case class RenderAlternative(w: Int, h:Int, children: List[RenderNode]) extends RenderGroup
case class RenderArbitrary(w: Int, h:Int, children: List[RenderNode]) extends RenderGroup
case class RenderSequence(w: Int, h:Int, children: List[RenderNode]) extends RenderGroup
case class RenderSometimeSequence(w: Int, h:Int, children: List[RenderNode]) extends RenderGroup
case class RenderOther(w: Int, h:Int, children: List[RenderNode]) extends RenderGroup
case class RenderHierarchy(w:Int, h:Int, sop: Hierarchy) extends RenderNode


case class Pos(x: Int, y: Int)

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

    def op(opname: String, x: Int, y: Int) =
      <.span(
        // extreme stylesheeting
        ^.className := (
          new SopMakerCSS.Position(
            x,
            y
          )
        ).position.htmlClass,
        SopMakerCSS.sopComponent,

        OnDragMod(handleDrag(opname)),
        OnDropMod(handleDrop(opname)),
        svg.svg(
          svg.width := 40,
          svg.height := 40,
          svg.rect(
            svg.x:=0,
            svg.y:=0,
            svg.width:=40,
            svg.height:=40,
            svg.rx:=5, svg.ry:=5,
            svg.fill := "white",
            svg.stroke:="black",
            svg.strokeWidth:=1
          ),
          svg.text(
            svg.x:="50%",
            svg.y:="50%",
            svg.textAnchor:="middle",
            svg.dy:=".3em", opname
          )
        )
      )

    def render(state: State) = {
      val ops = List(Operation("op1"), Operation("op2"))
      val idm = ops.map(o=>o.id -> o).toMap

      val fakeSop = Sequence(List(
        Parallel(List(SOP(Operation("parallel1")), SOP(Operation("Parallel2")))),
        Sequence(List(SOP(ops(1)), SOP(ops(0))))
      ))

      println(traverseTree(fakeSop))

      <.div(
        <.h2("Insert sop here:"),
        OnDataDrop(string => Callback.log("Received data: " + string)),
        <.br(),
        getRenderTree(traverseTree(fakeSop), 0, 0)
      )
    }

    def getRenderTree(node: RenderNode, xOffset: Int, yOffset: Int): TagMod = {
      println(node)
      node match {
        case n: RenderParallel => <.div("look a parallel")
          <.div("parallel",
            n.children.map(c => getRenderTree(c, xOffset, yOffset + 1)).toTagMod
          )
        case n: RenderSequence =>
          <.div("sequence",
            n.children.map(c => getRenderTree(c, xOffset, yOffset + 2)).toTagMod
          )
        case n: RenderHierarchy => op(n.sop.operation.toString, xOffset*100, yOffset*100)
        case _ => <.div("shuold not happen right now")
      }
    }

    def traverseTree(sop: SOP): RenderNode = {
      sop match {
        case s: Parallel => RenderParallel(
          w = getTreeWidth(s),
          h = getTreeHeight(s),
          children = sop.sop.collect{case e => traverseTree(e)}
        )
        case s: Sequence => RenderSequence(
          w = getTreeWidth(s),
          h = getTreeHeight(s),
          children = sop.sop.collect{case e => traverseTree(e)}
        )
        case s: Hierarchy => RenderHierarchy(1,1, s)
      }
    }

    def getTreeWidth(sop: SOP): Int = {
      sop match {
        // groups are as wide as the sum of all children widths
        case s: Parallel => s.sop.map(e => getTreeWidth(e)).foldLeft(0)(_ + _)
        case s: Sequence => { // sequences are as wide as their widest elements
          if(s.sop.isEmpty) 0
          else math.max(getTreeWidth(s.sop.head), getTreeWidth(Sequence(s.sop.tail)))
        }
        case s: Hierarchy => 1
      }
    }

    def getTreeHeight(sop: SOP): Int = {
      1 // TODO: implement
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
