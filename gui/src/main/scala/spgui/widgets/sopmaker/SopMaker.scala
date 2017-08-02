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
  val opHeight = 80f
  val opWidth = 80f
  val opSpacingX = 10f
  val opSpacingY = 10f

  case class Hover (
    currentMouseOver: String = "no_target",
    data: String = "nothing",
    currentlyDragging: Boolean = false
  )

  case class State(drag: String, drop: String, sop: List[String], hover: Hover)

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

    val wTemp = getTreeWidth(ExampleSops.giantSop)
    val hTemp = getTreeHeight(ExampleSops.giantSop)
    val paddingTop = 40f
    val paddingLeft = 40f

    def op(opname: String, x: Float, y: Float) =
      svg.svg(
        ^.onMouseOver --> handleMouseOver( opname ),
        ^.onMouseLeave --> handleMouseLeave( opname ),
        SopMakerCSS.sopComponent,
        //svg.width := "100%",
        //svg.height := "100%",
        //      SopMakerCSS.uh,
        ^.draggable := false,
        svg.svg(
          ^.draggable := false,
          svg.width := opWidth.toInt,
          svg.height:= opHeight.toInt,
          svg.x := x.toInt,
          svg.y := y.toInt,
          svg.rect(
            ^.draggable := false,
            //OnDragMod(handleDrag(opname)),
            //OnDropMod(handleDrop(opname)),
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
            ^.draggable := false,
            SopMakerCSS.opText,
            svg.text(
              svg.x := "50%",
              svg.y := "50%",
              svg.textAnchor := "middle",
              svg.dy := ".3em", opname
            )
          )
        )
      )

    def parallelBars(x: Float, y: Float, w:Float) =
      svg.svg(
        SopMakerCSS.sopComponent,
        svg.width := "100%",
        svg.height := "100%",
        svg.svg(
          SopMakerCSS.sopComponent,
          svg.width := w.toInt,
          svg.height := 12,
          svg.rect(
            svg.x := (x + opWidth/2).toInt,
            svg.y := y.toInt,
            svg.width:=w.toInt,
            svg.height:=4,
            svg.fill := "black",
            svg.strokeWidth:=1
          ),
          svg.rect(
            svg.x := (x + opWidth/2).toInt,
            svg.y := y.toInt + 8,
            svg.width:=w.toInt,
            svg.height:=4,
            svg.fill := "black",
            svg.strokeWidth:=1
          )
        )
      )
    
    val idm = ExampleSops.ops.map(o=>o.id -> o).toMap

    def handleMouseOver(zone: String): Callback = {
      $.modState(s =>
        s.copy(hover = s.hover.copy(currentMouseOver = zone))
        
      )
    }

    def handleMouseLeave(zone: String): Callback = {
      $.modState(s =>
        if(!s.hover.currentlyDragging) s.copy(hover = s.hover.copy(currentMouseOver = "no_target"))
        else s
      )
    }

    def handleMouseDown(h: Hover): Callback = {
      //println(h)
      $.modState(s =>
        s.copy(
          hover = s.hover.copy(
            currentlyDragging = (s.hover.currentMouseOver != "no_target"),
            data = h.currentMouseOver
          )
        )
      )
    }

    def handleMouseUp(h: Hover): Callback = {
      Callback({
        $.modState(s => s.copy(hover = s.hover.copy(
          data = "nothing",
          currentlyDragging = false
        ))).runNow()
        println("dragged " + h.data + " onto " + h.currentMouseOver)
      })
    }

    def handleMouseLeftWidget(h: Hover): Callback = {
      Callback({
        $.modState(s => s.copy(hover = Hover())).runNow()
        println("resetting hover state")
      })
    }

    def render(state: State) = {
      <.div(
        SopMakerCSS.hmm,
        <.div(state.hover.currentMouseOver),
        <.div(state.hover.data),
        <.div(state.hover.currentlyDragging.toString),
        //OnDataDrop(string => Callback.log("Received data: " + string)),
        svg.svg(
          ^.onMouseDownCapture --> handleMouseDown(state.hover),
          ^.onMouseUp --> handleMouseUp(state.hover),
          ^.onMouseLeave --> handleMouseLeftWidget(state.hover),
          svg.width := (wTemp + paddingLeft* 2).toInt,
          svg.height := ( hTemp + paddingTop * 2).toInt,
          
          //    OnDragMod(handleDrag("ssfdsdfsadfasdfasd")),
          //    OnDropMod(handleDrop("sdfsdf")),
          getRenderTree(
            traverseTree( ExampleSops.giantSop),
            getTreeWidth( ExampleSops.giantSop)*0.5f + paddingLeft,
            paddingTop
          )
        )
      )
    }
    
    def getRenderTree(node: RenderNode, xOffset: Float, yOffset: Float): TagMod = {
      node match {
        case n: RenderParallel => {
          var w = 0f
          svg.svg(
            parallelBars(xOffset - n.w/2 + opSpacingX/2, yOffset,n.w - opSpacingX),
            n.children.collect{case e: RenderNode => {
              val child = getRenderTree(
                e,
                xOffset + w + e.w/2 - n.w/2 + opSpacingX,
                yOffset  + parallelBarHeight + opSpacingY
              )
              w += e.w
              child
            }}.toTagMod,
            parallelBars(
              xOffset - n.w/2 + opSpacingX/2,
              yOffset + n.h - parallelBarHeight - opSpacingY,
              n.w - opSpacingX)
          )
        }
        case n: RenderSequence =>  getRenderSequence(n, xOffset, yOffset)
          
        case n: RenderHierarchy => {
          val opname = idm.get(n.sop.operation).map(_.name).getOrElse("[unknown op]")
          op(opname, xOffset, yOffset)
        }
        case _ => <.span("shuold not happen right now")
      }
    }

    def getRenderSequence(seq: RenderSequence, xOffset: Float, yOffset: Float): TagMod = {
      var h = yOffset
      seq.children.collect{case q: RenderSequenceElement => {
        h += q.h
        getRenderTree( q.self, xOffset, h - q.h )
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
      }
    }

    def onUnmount() = Callback {
      println("Unmounting sopmaker")
    }
  }

  private val component = ScalaComponent.builder[Unit]("SopMakerWidget")
    .initialState(State(drag = "", drop = "", sop = List(), hover = Hover()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}
