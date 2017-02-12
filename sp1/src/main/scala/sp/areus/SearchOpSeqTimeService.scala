package sp.areus

import akka.actor._
import sp.areus.CreateGanttChart.TimeUnit
import sp.domain._
import sp.system._
import sp.system.{ServiceLauncher, SPService}
import sp.system.messages._
import sp.domain.Logic._

import scala.util.Try

/**
 * Created by patrik on 2015-10-05.
 */

object CreateGanttChart extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Creates SOP for operations with shortast* execution time"
    ),
    "setup" -> SPAttributes(
      "ops" -> KeyDefinition("List[ID]", List(), Some(SPValue(List()))),
      "iterations" -> KeyDefinition("Int", List(10, 50, 100, 200, 500), Some(100)),
      "name" -> KeyDefinition("String", List(), Some("Result"))
    )
  )

  type TimeUnit = Double

  val transformTuple = (TransformValue("setup", _.getAs[SearchOpSeqTimeSetup]("setup")))

  val transformation = transformToList(transformTuple.productIterator.toList)

  def props = ServiceLauncher.props(Props(classOf[CreateGanttChart]))

}

case class SearchOpSeqTimeSetup(ops: List[ID], iterations: Int, name : String)

// TODO: Fix to not include complete operation in Gantt Attributes, i.e. op: ID
case class Gantt(rows: List[GanttRow])
case class GanttRow(ganttOperations: List[GanttOperation])
case class GanttOperation(op: Operation, start: Double, end: Double)

class CreateGanttChart extends Actor with ServiceSupport with CalcMethods {

  def receive = {
    case r@Request(service, attr, ids, reqID) =>

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)
      println(s"service: $service")

      val setup = transform(CreateGanttChart.transformTuple)

      //Example-------------------------------------------------------------
      //      val o11 = Operation("o11", List(), SPAttributes("time" -> 2))
      //      val o12 = Operation("o12", List(), SPAttributes("time" -> 5))
      //      val o13 = Operation("o13", List(), SPAttributes("time" -> 4))
      //      val o21 = Operation("o21", List(), SPAttributes("time" -> 1.5))
      //      val o22 = Operation("o22", List(), SPAttributes("time" -> 2))
      //      val o23 = Operation("o23", List(), SPAttributes("time" -> 4.23))
      //      val ops = List(o11, o12, o13, o21, o22, o23)
      //
      //      val sopSeq = SOP(Sequence(o11, o12, o13), Sequence(o21, o22, o23))
      //      val sopArbi = SOP(Arbitrary(o12, o22))
      //
      //      val conditions = sp.domain.logic.SOPLogic.extractOperationConditions(List(sopSeq, sopArbi), "traj")
      //      val opsUpd = ops.map { o =>
      //        val cond = conditions.get(o.id).map(List(_)).getOrElse(List())
      //        o.copy(conditions = cond)
      //      }

      //Init defs-----------------------------------------------------------------
      lazy val ops = ids.filter(o =>
        o.isInstanceOf[Operation] &&
        o.attributes.getAs[TimeUnit]("time").isDefined
      ).map(_.asInstanceOf[Operation])

      def mapOps[T](t: T) = ops.map(o => o.id -> t).toMap

      lazy val initState = State(mapOps(OperationState.init))
      def isGoalState(state: State) = {
        lazy val goalState = State(mapOps(OperationState.finished))
        state.state.forall { case (id, value) =>
          goalState.get(id) match {
            case Some(otherValue) => value == otherValue
            case _ => false
          }
        }
      }

      lazy val evalualteProp2 = EvaluateProp(mapOps((_: SPValue) => true), Set(), TwoStateDefinition)
      lazy val evalualteProp3 = EvaluateProp(mapOps((_: SPValue) => true), Set(), ThreeStateDefinition)

      //Calculation -------------------------------------------------------
      progress ! SPAttributes("progress" -> "Iterate sequences")
      val result = for {
        n <- 0 to setup.iterations
        straightSeq <- findStraightSeq(ops, initState, isGoalState, evalualteProp2)
        if straightSeq.forall(_.attributes.getAs[TimeUnit]("time").isDefined)
      } yield {
          val g = makeGantt(straightSeq, initState, evalualteProp3)
          val d = getDuration(g)
          (d, g)
        }


      //Response-------------------------------------------------------------------------------
      progress ! SPAttributes("progress" -> "Conclude result")
      if (result.isEmpty) {
        rnr.reply ! SPError("No result could be found.\n Are the conditions for the operations right?\n Do all operations include a correct attribute for its duration?")
      } else {

        val bestGantt = result.minBy(_._1)

        val opsG = bestGantt._2.rows.flatMap(_.ganttOperations)
        val ops = opsG.map { opG =>
          val attr = SPAttributes("gantt" -> SPAttributes("start" -> opG.start, "end" -> opG.end))
          opG.op.copy(attributes = (opG.op.attributes + attr))
        }
        
        val ganttSpec = SPSpec("Trajectory Gantt",
          SPAttributes("gantt" -> bestGantt._2.rows.map(_.ganttOperations.map(go => 
            SPAttributes("operation"->go.op.id, "start"->go.start, "end"->go.end)
          )),
            "totalTime" -> bestGantt._1)
        )

        rnr.reply ! Response(ops :+ ganttSpec, SPAttributes("info" -> s"created a gantt"), service, reqID)
      }
      progress ! PoisonPill
      self ! PoisonPill

    case (r: Response, reply: ActorRef) =>
      reply ! r

    case x =>
      sender() ! SPError("What do you want me to do? " + x)
      self ! PoisonPill
  }

    
}

case class ParSeqPlusDuration(parSeq: Seq[Set[Operation]], duration: TimeUnit, seq: Seq[Operation])

sealed trait CalcMethods {
  def findStraightSeq(ops: List[Operation], initState: State, isGoalState: State => Boolean, evalSetup: EvaluateProp): Option[Seq[Operation]] = {
    implicit val es = evalSetup

    def getEnabledOperations(state: State) = ops.filter(_.eval(state))

    def iterate(currentState: State, resultingOpSeq: Seq[Operation] = Seq()): Option[Seq[Operation]] = {
      if (isGoalState(currentState)) {
        Some(resultingOpSeq.reverse)
      } else {
        lazy val enabledOps = getEnabledOperations(currentState)
        if (enabledOps.isEmpty) {
          None
        } else {
          import scala.util.Random
          lazy val selectedOp = Random.shuffle(enabledOps).head
          iterate(selectedOp.next(currentState), selectedOp +: resultingOpSeq)
        }
      }
    }

    //Method starts
    iterate(initState)
  }


  def opToGantt(o: Operation, start: Double) = {
    val duration = o.attributes.getAs[Double]("time").getOrElse{println("fel getting time"); 1.0}
    GanttOperation(o, start, start + duration)
  }
  def getGanttTime(gantt: Gantt, lowest: Double) = {
    gantt.rows.foldLeft(lowest){(t, row)=>
      val rowTime = row.ganttOperations.last.start
      if (rowTime > t) rowTime else t
    }
  }

  def makeGantt(opsInStraightSeq: Seq[Operation], initState: State, evalSetup3: EvaluateProp) = {
    implicit val es3 = evalSetup3
    def itr(ops: List[Operation], state: State, gantt: Gantt): Gantt = {
      ops match {
        case Nil => gantt
        case x :: xs if x.eval(state) =>
          val startTime = getGanttTime(gantt, 0.0)
          val g = gantt.copy(rows = gantt.rows :+ GanttRow(List(opToGantt(x, startTime))))
          itr(xs, x.next(state), g)
        case x :: xs =>
          val g = gantt.rows.foldLeft((Gantt(List()), state, -1, 0.0)) { (opt, r) =>
            val theGantt = opt._1
            val theState = opt._2
            val foundIt = opt._3 != -1
            val currentEndTime = opt._4
            if (foundIt) {
              (theGantt.copy(rows = theGantt.rows :+ r), theState, 1, currentEndTime)
            }
            else {
              val newState = r.ganttOperations.last.op.next(theState)
              val onlyThisRowState = r.ganttOperations.last.op.next(state)
              if (x.eval(newState)) {
                val startTime = {
                  val t = getGanttTime(gantt, r.ganttOperations.last.end)
                  if (!x.eval(onlyThisRowState) && currentEndTime > t) currentEndTime
                  else t
                }
                val stateWithX = if (x.eval(onlyThisRowState)) x.next(onlyThisRowState) else x.next(newState)

                val newG = theGantt.copy(rows = theGantt.rows :+ r.copy(ganttOperations = r.ganttOperations :+ opToGantt(x, startTime)))
                (newG, stateWithX, 1, 0.0)
              } else {
                val rT = r.ganttOperations.last.end
                (theGantt.copy(rows = theGantt.rows :+ r), newState, -1, {if (currentEndTime > rT) currentEndTime else rT})
              }
            }
          }

          itr(xs, g._2, g._1)


      }
    }
    if (opsInStraightSeq.isEmpty){
      Gantt(List())
    } else {
      val x = opsInStraightSeq.head
      val xs = opsInStraightSeq.tail.toList
      val initG = Gantt(List(GanttRow(List(opToGantt(x, 0.0)))))
      itr(xs, x.next(initState), initG)
    }
  }

  def getDuration(gantt: Gantt) = {
    gantt.rows.foldLeft(0.0){(dur, r)=>
      val last = r.ganttOperations.last.end
      if (dur > last) dur else last
    }
  }

  def makeParallel(opsInStraightSeq: Seq[Operation], initState: State, evalSetup2: EvaluateProp, evalSetup3: EvaluateProp): Option[Seq[Set[Operation]]] = {

    def runOps(ops: List[Operation], startState: State): State = {
    implicit val e2 = evalSetup2
      ops.foldLeft(startState){(state, op) =>
        op.next(state)
      }
    }

    implicit val es3 = evalSetup3
    def iterate(remainingOps: List[Operation], currentState: State, parSeq: Seq[Set[Operation]]): Option[Seq[Set[Operation]]] = {
      remainingOps match {
        case Nil if parSeq.isEmpty => None
        case Nil => Some(parSeq)
        case x :: Nil => Some(parSeq :+ Set(x))
        case x :: xs =>
          val newState = x.next(currentState)
          val opsThatCanStart = x :: xs.takeWhile(_.eval(newState))
          val opsLeft = xs.filter(!opsThatCanStart.contains(_))
          iterate(opsLeft, runOps(opsThatCanStart, currentState), parSeq :+ opsThatCanStart.toSet)
        case _ => Some(parSeq.reverse)
      }
    }

    //Method starts
    val res = iterate(opsInStraightSeq.toList, initState, Seq())
    //println(s"result from par: $res")
    res
  }

  def findNewSeqRelations(sequences: List[List[Operation]]) = {
    def itr(xs: List[Operation], rel: Map[Set[Operation], SOP]): Map[Set[Operation], SOP] = {
      xs match {
        case Nil => rel
        case x :: Nil => rel
        case x :: xs =>
          val newMap = xs.map{ op =>
            var set = Set(x, op)
            rel.get(set) match {
              case Some(EmptySOP) => set -> Sequence(x, op)
              case Some(seq: Sequence) if seq.sop.head != x =>
                set -> Parallel(x, op)
              case Some(x) => set -> x
            }
          }
          newMap.foldLeft(rel){(map, tuple) =>
            map + tuple
          }
      }
    }
    def addConditions(map: Map[Set[Operation], SOP]) = {
      val ops = map.keys.flatten.toList
      val onlySeq = map.filter(_._2.isInstanceOf[Sequence]).values
      val upd = onlySeq.map{sop =>
        sop.sop.toList match {
          case (o1: Hierarchy) :: (o2: Hierarchy) :: Nil =>
            val cond = PropositionCondition(EQ(SVIDEval(o1.operation), ValueHolder(SPValue("f"))),
              List(), SPAttributes("kind"-> "precondition", "group"->"optimalSequence"))
            o2.operation -> cond
        }
      }.foldLeft(Map[ID, List[PropositionCondition]]()){(map, tuple)=>
        map + (tuple._1 -> (tuple._2 :: map.getOrElse(tuple._1, List[PropositionCondition]())))
      }
      ops.map{o =>
        if (upd.contains(o.id)){
          o.copy(conditions = upd(o.id) ) //o.conditions ++ upd(o.id))
        } else o
      }
    }



    val ops = sequences.flatten
    val relations: Map[Set[Operation], SOP] = (for {
      o1 <- ops
      o2 <- ops
    } yield (Set(o1, o2)-> EmptySOP)).toMap
    val opsRelations = sequences.foldLeft(relations){(rel, list) => itr(list, rel)}
    addConditions(opsRelations)


  }


  def duration(parSeq: Seq[Set[Operation]], durationKey: String): TimeUnit = {
    parSeq.foldLeft(0: TimeUnit) { case (acc, set) =>
      lazy val durationSet = set.flatMap(_.attributes.getAs[TimeUnit](durationKey))
      acc + durationSet.maxBy(identity)
    }
  }

  def translateToSOPSpec(parSeq: Seq[Set[Operation]], sopName: String): SOPSpec = {
    lazy val sop = Sequence(parSeq.map(set =>
      if (set.size > 1) {
        Parallel(set.toSeq.sortWith((o1, o2) => o1.name < o2.name).map(o => Hierarchy(o.id, List())): _*)
      } else {
        Hierarchy(set.head.id, List())
      }
    ): _*)
    SOPSpec(sopName, List(sop))
  }
}
