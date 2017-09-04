package spgui.widgets.sopmaker

import java.util.UUID
import japgolly.scalajs.react._

//import japgolly.scalajs.react.vdom.all.{ a, h1, h2, href, div, className, onClick, br, key }
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.svg
//import paths.mid.Bezier
//import paths.mid.Rectangle

//import spgui.components.DragAndDrop.{ OnDragMod, OnDropMod, DataOnDrag, OnDataDrop }

import spgui.communication._
import sp.domain._
import sp.domain.Logic._
import scalacss.ScalaCssReact._
import java.util.UUID

sealed trait RenderNode {
  val w: Float
  val h: Float
}

sealed trait RenderGroup extends RenderNode {
  val children: List[RenderNode]
}

case class RenderParallel(w: Float, h:Float, children: List[RenderNode])            extends RenderGroup
case class RenderAlternative(w: Float, h:Float, children: List[RenderNode])         extends RenderGroup
case class RenderArbitrary(w: Float, h:Float, children: List[RenderNode])           extends RenderGroup
case class RenderSometimeSequence(w: Float, h:Float, children: List[RenderNode])    extends RenderGroup
case class RenderOther(w: Float, h:Float, children: List[RenderNode])               extends RenderGroup
case class RenderSequence(w: Float, h:Float, children: List[RenderSequenceElement]) extends RenderGroup
case class RenderSequenceElement(w: Float, h:Float, self: RenderNode)               extends RenderNode
case class RenderHierarchy(w:Float, h:Float, sop: OperationNode)                        extends RenderNode



object SopMakerWidget {

  val parallelBarHeight = 12f
  val opHeight = 80f
  val opWidth = 80f
  val opSpacingX = 10f
  val opSpacingY = 10f

  case class Hover (
    fromId: UUID = null,
    toId: UUID = null,
    currentlyDragging: Boolean = false
  )

  case class State(sop: SOP, hover: Hover)

  val idm = ExampleSops.ops.map(o=>o.id -> o).toMap


  private class Backend($: BackendScope[Unit, State]) {

    /*
     val eventHandler = BackendCommunication.getMessageObserver(
     mess => {
     println("[SopMaker] Got event " + mess.toString)
     },
     "events"
     )
     */

    def render(state: State) = {
      //println(state.sop)
      <.div(
        SopMakerCSS.noSelect,

        // hover logic debug divs
        <.div(
          if(state.hover.fromId != null) state.hover.fromId.toString else "nop"),
        <.div(if(state.hover.toId != null)state.hover.toId.toString else "nop"),
        <.div(state.hover.currentlyDragging.toString),
        //<.div(state.sop.toString),

        svg.svg(
          ^.onMouseDownCapture --> handleMouseDown(state.hover),
          ^.onMouseUp --> handleMouseUp(state.hover),
          ^.onMouseLeave --> handleMouseLeftWidget(state.hover),
          svg.width := (getTreeWidth(state.sop) + paddingLeft* 2).toInt,
          svg.height := ( getTreeHeight(state.sop) + paddingTop * 2).toInt,
          getRenderTree(
            traverseTree( state.sop ),
            getTreeWidth( state.sop ) * 0.5f + paddingLeft,
            paddingTop
          )
        )
      )
    }

    // def handleDrag(drag: String)(e: ReactDragEventFromInput): Callback = {
    //   Callback({
    //     e.dataTransfer.setData("json", drag)
    //   })
    // }

    // def handleDrop(drop: String)(e: ReactDragEvent): Callback = {
    //   val drag = e.dataTransfer.getData("json")
    //   Callback.log("Dropping " + drag + " onto " + drop)
    // }

    val paddingTop = 40f
    val paddingLeft = 40f

    def op(opId: UUID, opname: String, x: Float, y: Float) =
      svg.svg(
        ^.onMouseOver --> handleMouseOver( opId ),
        ^.onMouseLeave --> handleMouseLeave( opId ),
        SopMakerCSS.sopComponent,
        ^.draggable := false,
        svg.svg(
          ^.draggable := false,
          svg.width := opWidth.toInt,
          svg.height:= opHeight.toInt,
          svg.x := x.toInt,
          svg.y := y.toInt,
          svg.rect(
            ^.draggable := false,
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

    def handleMouseOver(zoneId: UUID): Callback = {
      $.modState(s =>
        s.copy(hover  = s.hover.copy(toId = zoneId))
      )
    }

    def handleMouseLeave(zoneId: UUID): Callback = {
      $.modState(s =>
        if(!s.hover.currentlyDragging) s.copy(hover = s.hover.copy(toId = null))
        else s
      )
    }

    def handleMouseDown(h: Hover): Callback = {
      $.modState(s =>
        s.copy(
          hover = s.hover.copy(
            currentlyDragging = (s.hover.toId != null),
            fromId = h.toId
          )
        )
      )
    }

    def handleMouseUp(h: Hover): Callback = {
      Callback({
        $.modState(s => {
          s.copy(
            sop = insertSop(
              root = s.sop,
              targetId = s.hover.toId,
              sopId = s.hover.fromId
            ),
            hover = s.hover.copy(
              toId = null,
              currentlyDragging = false
            ))
        }).runNow()
        println("dragged " + h.fromId + " onto " + h.toId)
      })
    }

    def handleMouseLeftWidget(h: Hover): Callback = {
      Callback({
        $.modState(s => s.copy(hover = Hover())).runNow()
        println("resetting hover state")
      })
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
          op(n.sop.nodeID, opname, xOffset, yOffset)
        }
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
        case s: OperationNode => RenderHierarchy(
          w = getTreeWidth(s),
          h = getTreeHeight(s),
          sop = s
        )
      }
    }

    def traverseSequence(s: Sequence): RenderSequence = {
      if(s.sop.isEmpty) null else RenderSequence(
        h = getTreeHeight(s),
        w = getTreeWidth(s),
        children = s.sop.collect{case e: SOP => {
          RenderSequenceElement(
            getTreeWidth(e),
            getTreeHeight(e),
            traverseTree(e)
          )
        }}
      )
    }

    def getTreeWidth(sop: SOP): Float = {
      sop match {
        // groups are as wide as the sum of all children widths + its own padding
        case s: Parallel => s.sop.map(e => getTreeWidth(e)).sum + opSpacingX *2
        case s: Sequence => { // sequences are as wide as their widest elements
          if(s.sop.isEmpty) 0
          else math.max(getTreeWidth(s.sop.head), getTreeWidth(Sequence(s.sop.tail)))
        }
        case s: OperationNode => {
          opWidth + opSpacingX
        }
      }
    }

    def getTreeHeight(sop: SOP): Float = {
      sop match  {
        case s: Parallel => {
          if(s.sop.isEmpty) 0
          else math.max(
            getTreeHeight(s.sop.head) + (parallelBarHeight*2 + opSpacingY*2),
            getTreeHeight(Parallel(s.sop.tail))
          )
        }
        case s: Sequence => {
          s.sop.map(e => getTreeHeight(e)).foldLeft(0f)(_ + _)
        }
        case s: OperationNode => opHeight + opSpacingY
      }
    }

    def onUnmount() = Callback {
      println("Unmounting sopmaker")
    }

    def sopList(root: SOP): List[SOP] = {
      root :: root.sop.map(e => sopList(e)).toList.flatten
    }

    def findSop(root: SOP, sopId: UUID): SOP = {
      println(sopList(root).filter(x => x.nodeID != sopId).head)
      sopList(root).filter(x => x.nodeID == sopId).head
    }

    def insertSop(root: SOP, targetId: UUID, sopId: UUID): SOP = {
      if(root.nodeID == targetId) {
        root match {
          case r: Parallel =>
            Parallel(sop = findSop($.state.runNow().sop, sopId) :: r.sop)
          case r: Sequence =>
            Sequence(sop = findSop($.state.runNow().sop, sopId) :: r.sop)
          case r: OperationNode => Parallel(
            sop = List(r, findSop($.state.runNow().sop, sopId))) // $.state abuse
        }
      } else {
        root match {
          case r: Parallel =>
            Parallel(sop = r.sop.collect{case e => insertSop(e, targetId, sopId)})
          case r: Sequence =>
            Sequence(sop = r.sop.collect{case e => insertSop(e, targetId, sopId)})
          case r: OperationNode => r // TODO
        }
      }
    }
  }


  private val component = ScalaComponent.builder[Unit]("SopMakerWidget")
    .initialState(State(sop = ExampleSops.giantSop, hover = Hover()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}
