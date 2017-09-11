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
  val opWidth = 80f
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

  trait Rect extends js.Object {
    var left: Float = js.native
    var top: Float = js.native
    var width: Float = js.native
    var height: Float = js.native
  }

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
        ^.onMouseOver ==> handleMouseOver("sop_style"),
        ^.onMouseOut ==> handleMouseOver("not_sop_style"),
        SopMakerCSS.sopContainer,
        getRenderTree(
          traverseTree( state.sop ),
          getTreeWidth( state.sop ) * 0.5f + paddingLeft,
          paddingTop
        ).toTagMod
      )
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
      val handle = makeHandle(opId)
      <.span(
        SPWidgetElements.draggable(opname, opId, "sop"),

        SopMakerCSS.sopComponent,
        ^.style := {
          var rect =  (js.Object()).asInstanceOf[Rect]
          rect.left = x
          rect.top = y
          rect.height = opHeight
          rect.width = opWidth
          rect
        },
        svg.svg(
          svg.svg(
            svg.width := opWidth.toInt,
            svg.height:= opHeight.toInt,
            svg.x := 0,
            svg.y := 0,
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
                svg.dy := ".3em", opname
              )
            )
          )
        )
      )
    }

    def parallelBars(x: Float, y: Float, w:Float): TagMod =
      <.span(
        SopMakerCSS.sopComponent,
        ^.style := {
          var rect =  (js.Object()).asInstanceOf[Rect]
          rect.left = x + opWidth/2
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

    def dropZone(  id: UUID, x: Float, y: Float, w: Float, h: Float) =
      spgui.components.SPWidgetElements.DragoverZone(id, x, y, w, h)

    def handleMouseOver(zoneId: UUID): Callback = {
      $.modState(s =>
        s.copy(hoverData = HoverData(zoneId, s.hoverData.dragging))
      )
    }

    def handleMouseLeave(zoneId: UUID): Callback = {
      $.modState(s => {
        if(s.hoverData.position == zoneId) s.copy()
         else s.copy(hoverData = HoverData(zoneId))
      })
    }

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
          List(parallelBars(xOffset - n.w/2 + opSpacingX/2, yOffset,n.w - opSpacingX)) ++
          children ++
          List(parallelBars(
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
    .initialState(State(sop = ExampleSops.giantSop, hoverData = HoverData()))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = spgui.SPWidget(spwb => component())
}
