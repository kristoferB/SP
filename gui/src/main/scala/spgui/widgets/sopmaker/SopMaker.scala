package spgui.widgets.sopmaker

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
import spgui.components.SPWidgetElements
import spgui.dragging._

sealed trait RenderNode {
  val nodeId: UUID
  val w: Float
  val h: Float
}

sealed trait RenderGroup extends RenderNode {
  val children: List[RenderNode]
}

case class RenderParallel(
  nodeId: UUID, w: Float, h:Float, children: List[RenderNode]) extends RenderGroup
case class RenderAlternative(
  nodeId: UUID, w: Float, h:Float, children: List[RenderNode]) extends RenderGroup
case class RenderArbitrary(
  nodeId: UUID, w: Float, h:Float, children: List[RenderNode]) extends RenderGroup
case class RenderSometimeSequencenode(
  nodeId: UUID, w: Float, h:Float, children: List[RenderNode]) extends RenderGroup
case class RenderOther(
  nodeId: UUID, w: Float, h:Float, children: List[RenderNode]) extends RenderGroup
case class RenderSequence(
  nodeId: UUID, w: Float, h:Float, children: List[RenderSequenceElement]) extends RenderGroup
case class RenderSequenceElement(
  nodeId: UUID, w: Float, h:Float, self: RenderNode) extends RenderNode
case class RenderOperationNode(
  nodeId: UUID, w:Float, h:Float, sop: OperationNode) extends RenderNode

object SopMakerWidget {
  val parallelBarHeight = 12f
  val opHeight = 80f
  val opWidth = 120f
  val opSpacingX = 10f
  val opSpacingY = 10f

  var xOrigin = 0f
  var yOrigin = 0f

  case class HoverData(
    position: UUID = null,
    dragging: Boolean = false,
    dragPosition: (Float, Float) = (0,0)
  )

  case class State(sop: SOP, hoverData: HoverData)

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
      <.div(
        //SPWidgetElements.DragoverContext(),
        <.span(
        ^.onMouseOver ==> handleMouseOver("sop_style"),
        ^.onMouseOut ==> handleMouseOver("not_sop_style"),
        SopMakerCSS.sopContainer,
        getRenderTree(
          traverseTree( state.sop ),
          getTreeWidth( state.sop ) * 0.5f + paddingLeft,
          paddingTop
        ).toTagMod
      ))
    }

    def handleMouseOver(style:String)(e: ReactMouseEvent) = Callback {
      Dragging.setDraggingStyle(style)
    }

    val paddingTop = 40f
    val paddingLeft = 40f
    val handlePrefix = "drag-handle-"
    def makeHandle(id: UUID) = handlePrefix + id.toString
    def readHandle(handle: String) = UUID.fromString(handle.split(handlePrefix+"| ")(1))

    def op(opId: UUID, opname: String, x: Float, y: Float): TagMod = {
      <.span(
        ^.draggable := false,
        SPWidgetElements.draggable(opname, opId, "sop"),
        SopMakerGraphics.sop(opname, x.toInt, y.toInt)
      )
  }

    import spgui.circuit._
    import spgui.components.SPWidgetElements
    //val draggingConnection = SPGUICircuit.connect(x => (x.draggingState.dragging, x.draggingState.target))
          
    def dropZone(  id: UUID, x: Float, y: Float, w: Float, h: Float): TagMod =
      SPWidgetElements.DragoverZone(id, x, y, w, h)

    def getRenderTree(node: RenderNode, xOffset: Float, yOffset: Float): List[TagMod] = {
      node match {
        case n: RenderParallel => {
          var w = 0f
          
          var children = List[TagMod]()
          for(e <- n.children) {
            val child = getRenderTree(
              e,
              xOffset + w + e.w/2 - n.w/2 + opSpacingX,
              yOffset  + parallelBarHeight + opSpacingY
            )
            w += e.w
            children = children ++ child
          }
          List(dropZone(   // dropzone for the whole parallel
            id = n.nodeId,
            x = xOffset - n.w/2 + opSpacingX/2 + opWidth/2,
            y = yOffset,
            w = n.w,
            h = n.h
          )) ++
          List(
            SopMakerGraphics.parallelBars(xOffset - n.w/2 + opSpacingX/2, yOffset,n.w - opSpacingX)) ++
          children ++
          List(SopMakerGraphics.parallelBars(
            xOffset - n.w/2 + opSpacingX/2,
            yOffset + n.h - parallelBarHeight - opSpacingY,
            n.w - opSpacingX
          ))
        }
        case n: RenderSequence =>  getRenderSequence(n, xOffset, yOffset)
          
        case n: RenderOperationNode => {
          val opname = idm.get(n.sop.operation).map(_.name).getOrElse("[unknown op]")
          List(op(n.sop.nodeID, opname, xOffset, yOffset)) ++
          List(dropZone(
            id = n.nodeId,
            x = xOffset,
            y = yOffset,
            w = opWidth,
            h = opHeight
          ))
        }
      }
    }

    def getRenderSequence(seq: RenderSequence, xOffset: Float, yOffset: Float): List[TagMod] = {
      var h = yOffset
      var children = List[TagMod]()
      for(q <- seq.children){
        h += q.h
        children = children ++ getRenderTree( q.self, xOffset, h - q.h )
      }
      children
    }

    def traverseTree(sop: SOP): RenderNode = {
      sop match {
        case s: Parallel => RenderParallel(
          nodeId = s.nodeID,
          w = getTreeWidth(s),
          h = getTreeHeight(s),
          children = sop.sop.collect{case e => traverseTree(e)}
        )
        case s: Sequence => traverseSequence(s)
        case s: OperationNode => RenderOperationNode(
          nodeId = s.nodeID,
          w = getTreeWidth(s),
          h = getTreeHeight(s),
          sop = s
        )
      }
    }

    def traverseSequence(s: Sequence): RenderSequence = {
      if(s.sop.isEmpty) null else RenderSequence(
        nodeId = s.nodeID,
        h = getTreeHeight(s),
        w = getTreeWidth(s),
        children = s.sop.collect{case e: SOP => {
          RenderSequenceElement(
            nodeId = e.nodeID,
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
            sop = List(r, findSop($.state.runNow().sop, sopId)))
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
    .initialState(State(sop = ExampleSops.giantSop, hoverData = HoverData()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}
