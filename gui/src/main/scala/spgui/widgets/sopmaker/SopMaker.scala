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
  val w: Float
  val h: Float
}

trait RenderGroup extends RenderNode {
  val children: List[RenderNode]
}

case class RenderParallel(w: Float, h:Float, children: List[RenderNode]) extends RenderGroup
case class RenderAlternative(w: Float, h:Float, children: List[RenderNode]) extends RenderGroup
case class RenderArbitrary(w: Float, h:Float, children: List[RenderNode]) extends RenderGroup
case class RenderSometimeSequence(w: Float, h:Float, children: List[RenderNode]) extends RenderGroup
case class RenderOther(w: Float, h:Float, children: List[RenderNode]) extends RenderGroup
case class RenderHierarchy(w:Float, h:Float, sop: Hierarchy) extends RenderNode

case class RenderSequence(w: Float, h:Float, children: List[RenderSequenceElement]) extends RenderGroup
case class RenderSequenceElement(w: Float, h:Float, self: RenderNode) extends RenderNode

case class Pos(x: Float, y: Float)

object SopMakerWidget {
  val parallelBarHeight = 12f//45f
  val opHeight = 50f
  val opWidth = 60f
  val opSpacingX = 10f
  val opSpacingY = 10f

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

    def op(opname: String, x: Float, y: Float) = <.span(
      // extreme stylesheeting
      ^.className := (
        new SopMakerCSS.Position(
          x.toInt,
          y.toInt
        )
      ).position.htmlClass,
      SopMakerCSS.sopComponent,

      OnDragMod(handleDrag(opname)),
      OnDropMod(handleDrop(opname)),
      svg.svg(
        svg.width := opWidth.toInt,
        svg.height := opHeight.toInt,
        svg.rect(
          svg.x:=0,
          svg.y:=0,
          svg.width:=opWidth.toInt,
          svg.height:=opHeight.toInt,
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

    def parallelBars(x: Float, y: Float, w:Float) = <.span(
      ^.className := (
        new SopMakerCSS.Position(
          (x + opWidth/2).toInt,
          y.toInt
        )
      ).position.htmlClass,
      SopMakerCSS.sopComponent,
      svg.svg(
        svg.width := w.toInt,
        svg.height := 12,
        svg.rect(
          svg.x:=0,
          svg.y:=0,
          svg.width:=w.toInt,
          svg.height:=4,
          svg.rx:=0, svg.ry:=0,
          svg.fill := "black",
          svg.strokeWidth:=1
        ),
        svg.rect(
          svg.x:=0,
          svg.y:=8,
          svg.width:=w.toInt,
          svg.height:=4,
          svg.rx:=0, svg.ry:=0,
          svg.fill := "black",
          svg.strokeWidth:=1
        )
      )
    )

    var ops = List(
      Operation("op1"),
      Operation("op2"),
      Operation("op3"),
      Operation("op4"),
      Operation("op5"),
      Operation("op6"),
      Operation("op7"),
      Operation("op8"),
      Operation("op9"),
      Operation("op10"),
      Operation("op11"),
      Operation("test")
    )
    val idm = ops.map(o=>o.id -> o).toMap

    def render(state: State) = {
      val fakeSop = Sequence(List(
        Sequence(
          List(SOP(ops(7)), SOP(ops(8)))),
        Parallel(
          List(
            SOP(ops(0)),
            SOP(ops(0)),
            SOP(ops(1)),
            SOP(ops(2)),
            Parallel(
              List(
                SOP(ops(11)),
                SOP(ops(11)),
                Parallel(
                  List(
                    SOP(ops(11)),
                    SOP(ops(11))
                  )
                )
              )
            )
          )
        ),
        Parallel(
          List(
            Parallel(
              List(
                SOP(ops(11)),
                SOP(ops(11))
              )
            ),
            Parallel(
              List(
                SOP(ops(11)),
                SOP(ops(11))
              )
            ),
            Parallel(
              List(
                SOP(ops(11)),
                SOP(ops(11))
              )
            )
          )
        ),

        Parallel(
          List( SOP(ops(4)), SOP(ops(5)), SOP(ops(6)),
            Sequence(
              List(
                SOP(ops(7)),
                SOP(ops(7)),
                SOP(ops(8)),
                Parallel(
                  List( SOP(ops(9)), SOP(ops(10)) ))
              )
            ),
            SOP(ops(7))
          )),
        Sequence(
          List(SOP(ops(7)), SOP(ops(8)))),
        Parallel(
          List(
            SOP(ops(9)),
            Sequence(
              List(
                Parallel(
                  List( SOP(ops(9)), SOP(ops(10)))),
                Parallel(
                  List( SOP(ops(9)), SOP(ops(10)),SOP(ops(10))))
              ))
          )),
        Parallel(
          List( SOP(ops(9)), SOP(ops(10)), SOP(ops(10)) )),
        Parallel(
          List( SOP(ops(9)), SOP(ops(10)) ))
      ))

      println(traverseTree(fakeSop))

      <.div(
        <.h2("Insert sop here:"),
        OnDataDrop(string => Callback.log("Received data: " + string)),
        <.br(),
        getRenderTree(traverseTree(fakeSop), 100f, 100f)
      )
    }
    
    def getRenderTree(node: RenderNode, xOffset: Float, yOffset: Float): TagMod = {
      node match {
        case n: RenderParallel => {
          var w = 0f
          <.div("parallel",
            parallelBars(xOffset - n.w/2 + opSpacingX/2, yOffset,n.w -opSpacingX),
            n.children.collect{case e: RenderNode => {
              val child = getRenderTree(
                e,
                xOffset + w + e.w/2 -n.w/2 + opSpacingX, 
                yOffset  + parallelBarHeight + opSpacingY
              )
              w += e.w
              child
            }}.toTagMod,
            parallelBars(xOffset - n.w/2 +opSpacingX/2, yOffset + n.h-parallelBarHeight-opSpacingY,n.w -opSpacingX)
          )
        }
        case n: RenderSequence => <.div("sequence",
          getRenderSequence(n, xOffset, yOffset)
        )
        case n: RenderHierarchy => {
          val opname = idm.get(n.sop.operation).map(_.name).getOrElse("[unknown op]")
          op(opname, xOffset, yOffset)
        }
        case _ => <.div("shuold not happen right now")
      }
    }

    def getRenderSequence(seq: RenderSequence, xOffset: Float, yOffset: Float): TagMod = {
      var h = yOffset
      seq.children.collect{case q: RenderSequenceElement => {
        h += q.h
        <.div("sequence element",
          getRenderTree( q.self, xOffset, h - q.h )
        )
      }}.toTagMod
    }

    def traverseTree(sop: SOP): RenderNode = {
      sop match {
        case s: Parallel => RenderParallel(
          w = getTreeWidth(s),
          h = getTreeHeight(s),
          children = sop.sop.collect{case e => traverseTree(e)}
        )
        case s: Sequence => traverseSequence(s)
        case s: Hierarchy => RenderHierarchy(
          w = getTreeWidth(s),
          h = getTreeHeight(s),
          sop = s
        )
      }
    }

    def traverseSequence(s: Sequence): RenderSequence = {
      if(!s.sop.isEmpty) RenderSequence(
        h = getTreeHeight(s),
        w = getTreeWidth(s),
        children = s.sop.collect{case e: SOP => {
          RenderSequenceElement(
            getTreeWidth(e),
            getTreeHeight(e),
            traverseTree(e)
          )
        }}
      ) else null
    }

    def getTreeWidth(sop: SOP): Float = {
      sop match {
        // groups are as wide as the sum of all children widths + its own padding
        case s: Parallel => s.sop.map(e => getTreeWidth(e)).sum + opSpacingX *2
        case s: Sequence => { // sequences are as wide as their widest elements
          if(s.sop.isEmpty) 0
          else math.max(getTreeWidth(s.sop.head), getTreeWidth(Sequence(s.sop.tail)))
        }
        case s: Hierarchy => {
          opWidth + opSpacingX
        }
      }
    }

    def getTreeHeight(sop: SOP): Float = {
      sop match  {
        case s: Parallel => {
          if(s.sop.isEmpty) 0
          else math.max(
            getTreeHeight(s.sop.head) + (parallelBarHeight*2+opSpacingY*2),
            getTreeHeight(Parallel(s.sop.tail))
          )
        }
        case s: Sequence => {
          s.sop.map(e => getTreeHeight(e)).foldLeft(0f)(_ + _)
        }
        case s: Hierarchy => opHeight + opSpacingY
        case _ => {println("woops"); -1}
      }
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
