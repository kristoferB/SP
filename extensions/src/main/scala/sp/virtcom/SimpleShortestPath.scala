package sp.virtcom

import akka.actor._
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent.duration._
import sp.supremicaStuff.auxiliary.DESModelingSupport
import collection.immutable.SortedMap
import scala.math
import sp.services.sopmaker.MakeASop
import scala.util.{Success, Failure}

import org.json4s._
import scala.annotation.tailrec

case class Parameters(waitAllowed: Boolean, longest: Boolean)

object SimpleShortestPath extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Complete all operations asap. (Variables need to end up at initial state after the ops are completed)"
    ),
    "parameters" -> SPAttributes(
      "waitAllowed" -> KeyDefinition("Boolean", List(), Some(true)),
      "longest" -> KeyDefinition("Boolean", List(), Some(false))
    )
  )
  val transformTuple = (TransformValue("parameters", _.getAs[Parameters]("parameters")))
  val transformation = transformToList(transformTuple.productIterator.toList)
  def props() = ServiceLauncher.props(Props(classOf[SimpleShortestPath]))
}

class SimpleShortestPath extends Actor with ServiceSupport with DESModelingSupport with MakeASop {
  implicit val timeout = Timeout(100 seconds)

  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)
      progress ! SPAttributes("progress" -> "starting search")

      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get
      val params = transform(SimpleShortestPath.transformTuple)
      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
      val things = ids.filter(_.isInstanceOf[Thing]).map(_.asInstanceOf[Thing])

      val resultName = "Result_" + (if(params.longest) "long" else "short") + "_" + (if(params.waitAllowed) "wait" else "noWait")
      val rl = List(SOPSpec(resultName, List(dijkstra(things,ops,params.waitAllowed,params.longest))))
      replyTo ! Response(rl, SPAttributes(), rnr.req.service, rnr.req.reqID)
      self ! PoisonPill
      progress ! PoisonPill
    }
    case _ => sender ! SPError("Ill formed request");
  }

  // finish all ops asap
  def dijkstra(things: List[Thing], ops: List[Operation], waitAllowed: Boolean = true, longest: Boolean = false): SOP = {
    import de.ummels.prioritymap.PriorityMap
    val opsmap = ops.map(o=>o.id -> o).toMap
    val opsvars = ops.map(o => o.id -> sp.domain.logic.OperationLogic.OperationState.inDomain).toMap
    val statevars = things.map(sv => sv.id -> sv.inDomain).toMap ++ opsvars
    implicit val props = EvaluateProp(statevars, Set(), ThreeStateDefinition)
    val initState = State(getIdleState(things.toSet).state ++ ops.map(_.id -> OperationState.init).toMap)
    val goalState = State(getIdleState(things.toSet).state ++ ops.map(_.id -> OperationState.finished).toMap)
    val inf = Long.MaxValue
    case class Node(state: State, running: PriorityMap[ID, Long] = PriorityMap())
    val initNode = Node(initState)
    val goalNode = Node(goalState)

    def canStartOps(state: State) = {
      val initOps = ops.filter(o=>state(o.id)==OperationState.init)
      initOps.filter(_.conditions.filter(_.attributes.getAs[String]("kind").getOrElse("") == "precondition").headOption
        match {
        case Some(cond) => cond.eval(state)
        case None => true
      })
    }

    def getDur(o: Operation): Long =
      if(longest) 1000000 - (o.attributes.getAs[Double]("duration").getOrElse(0.0) * 100.0).round
      else (o.attributes.getAs[Double]("duration").getOrElse(0.0) * 100.0).round

    // adapted from Michael Ummels
    def search(active: PriorityMap[Node,Long], res: Map[Node, Long], pred: Map[Node, Node]):
        (Map[Node, Long], Map[Node, Node]) =
      if (active.isEmpty) (res, pred)
      else if(active.head._1 == goalNode) (res + active.head, pred) // only care about the goal...
      else {
        val (node,t) = active.head

        if(res.size > 0 && res.size % 1000 == 0) {
          println("------------------------------------------------------------------------------")
          println("Searched " + res.size + " nodes, " + active.size + " open nodes, at time: " + t)
        }
        val start = canStartOps(node.state).map(o => {
          (Node(o.next(node.state), node.running + (o.id -> (t + getDur(o)))),t)
        })
        val finish = if(node.running.nonEmpty) {
          val (opid,deadline) = node.running.head
          List((Node(opsmap(opid).next(node.state), node.running - opid),deadline))
        } else List()

        val potential = if(waitAllowed) start ++ finish else if(start.isEmpty) finish else start
        val betterNeighbors = potential.filter{case (n,nt) => !res.contains(n) && nt < active.getOrElse(n, inf)}
        search(active.tail ++ betterNeighbors, res + (node -> t), pred ++ betterNeighbors.map(_._1->node))
      }

    val t0 = System.nanoTime()
    val (res,pred) = search(PriorityMap(initNode -> 0), Map(), Map())
    val t1 = System.nanoTime()

    def findPath(curNode: Node, pred: Map[Node,Node], acum: List[Node]): List[Node] = {
      pred.get(curNode) match {
        case Some(src) =>
          findPath(src, pred, curNode :: acum)
        case None =>
          curNode::acum
      }
    }
    val path = findPath(goalNode, pred, List())

    def getTimes(path: List[Node], olr: Set[ID]=Set(), start: Map[ID, Long]=Map(),finish: Map[ID, Long]=Map()):
        (Map[ID, Long],Map[ID, Long]) = {
      path match {
        case x::xs =>
          val nr = x.running.map(_._1).toSet
          val s = nr.diff(olr)
          val f = olr.diff(nr)
          getTimes(xs,nr,start ++ s.map(_ -> res(x)).toMap,finish ++ f.map(_ -> res(x)).toMap)
        case Nil => (start,finish)
      }
    }
    val (start,finish) = getTimes(path)

    // sanity check
    ops.foreach { op =>
      val dur = getDur(op)
      // println(op.name + ": " + start(op.id) + " - " + dur + " --> " + finish(op.id))
      assert(finish(op.id) - start(op.id) - dur == 0)
    }

    def rel(op1: ID,op2: ID): SOP = {
      if(finish(op1) <= start(op2))
        Sequence(op1,op2)
      else if(finish(op2) <= start(op1))
        Sequence(op2,op1)
      else
        Parallel(op1,op2)
    }

    val pairs = (for {
      op1 <- ops
      op2 <- ops if(op1 != op2)
        } yield Set(op1.id,op2.id)).toSet

    val rels = pairs.map { x => (x -> rel(x.toList(0),x.toList(1))) }.toMap
    val sop = makeTheSop(ops.map(_.id), rels, EmptySOP)

    println("Goal state at t="+res(goalNode)+" found after " + (t1 - t0)/1E9 + "s and " + res.size + " searched nodes.")
     
    sop.head
  }
}
