package sp.areus

import akka.actor._
import sp.areus.SearchOpSeqTimeService.TimeUnit
import sp.domain._
import sp.system._
import sp.system.{ServiceLauncher, SPService}
import sp.system.messages._
import sp.domain.Logic._

/**
 * Created by patrik on 2015-10-05.
 */

object SearchOpSeqTimeService extends SPService {
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

  def props = ServiceLauncher.props(Props(classOf[SearchOpSeqTimeService]))

}

case class SearchOpSeqTimeSetup(ops: List[ID], iterations: Int, name : String)

class SearchOpSeqTimeService extends Actor with ServiceSupport with CalcMethods {

  def receive = {
    case r@Request(service, attr, ids, reqID) =>

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)
      println(s"service: $service")

      val setup = transform(SearchOpSeqTimeService.transformTuple)

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
      lazy val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])

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
      lazy val result = for {
        n <- 0 to setup.iterations
        straightSeq <- findStraightSeq(ops, initState, isGoalState, evalualteProp2)
        parSeq <- makeParallel(straightSeq, initState, evalualteProp2, evalualteProp3)
        if straightSeq.forall(_.attributes.getAs[TimeUnit]("time").isDefined)
      } yield {
          ParSeqPlusDuration(parSeq, duration(parSeq, durationKey = "time"))
        }

//      println(result.map(_.duration).distinct.sortBy(identity).mkString("\n"))
      lazy val bestParSeq = result.minBy(_.duration)


      //Response-------------------------------------------------------------------------------
      progress ! SPAttributes("progress" -> "Conclude result")
      if(result.isEmpty) {
        rnr.reply ! SPError("No result could be found.\n Are the conditions for the operations right?\n Do all operations include a correct attribute for its duration?")
      } else {

        var best = -1.0
        val allSOPs = result.flatMap { res =>
          val sopToWorkOn = translateToSOPSpec(bestParSeq.parSeq, setup.name)
          val durationAttr = SPAttributes("SOPduration" -> bestParSeq.duration)
          if (best == -1 || best >= bestParSeq.duration) {
            best = bestParSeq.duration
            Some(sopToWorkOn.copy(attributes = sopToWorkOn.attributes merge durationAttr))
          } else
            None
        }        




        lazy val sopToWorkOn = translateToSOPSpec(bestParSeq.parSeq, setup.name)
        lazy val durationAttr = SPAttributes("SOPduration" -> bestParSeq.duration)
        lazy val sopToReturn = sopToWorkOn.copy(attributes = sopToWorkOn.attributes merge durationAttr)

        rnr.reply ! Response(allSOPs.toList, SPAttributes("info" -> s"Added sop '${sopToReturn.name}'") merge durationAttr, service, reqID)
      }
      progress ! PoisonPill
      self ! PoisonPill

    case (r: Response, reply: ActorRef) =>
      reply ! r

    case x =>
      sender() ! SPError("What do you whant me to do? " + x)
      self ! PoisonPill
  }
}

case class ParSeqPlusDuration(parSeq: Seq[Set[Operation]], duration: TimeUnit)

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

  def makeParallel(opsInStraightSeq: Seq[Operation], initState: State, evalSetup2: EvaluateProp, evalSetup3: EvaluateProp): Option[Seq[Set[Operation]]] = {

    def isParallel(thisOp: Operation, thatOp: Operation, ii: State): Boolean = {
      implicit val es = evalSetup3

      if (!thisOp.eval(ii) || !thatOp.eval(ii)) return false
      val ei = thisOp.next(ii)
      if (!thatOp.eval(ei)) return false
      val fi = thisOp.next(ei)
      if (!thatOp.eval(fi)) return false
      val ie = thatOp.next(ii)
      if (!thisOp.eval(ie)) return false
      val if_ = thatOp.next(ie)
      if (!thisOp.eval(if_)) return false

      //Under the assumption that there are no post-guards between 'thisOp' and 'thatOp,
      //all remaining transitions are ok

      true
    }

    def iterate(remainingOps: Seq[Operation], currentState: State, parSeq: Seq[Set[Operation]]): Option[Seq[Set[Operation]]] = {
      remainingOps match {
        case o +: os =>
          if (parSeq.head.forall(otherO => isParallel(o, otherO, currentState))) {
            //Add 'o' to set
            iterate(os, currentState, (Set(o) ++ parSeq.head) +: parSeq.tail)
          } else {
            //Update currentState and add 'o' to a new 'parallel sop' in seq
            implicit val es = evalSetup2
            lazy val newState = parSeq.head.foldLeft(currentState) { case (stateAcc, otherO) => otherO.next(stateAcc) }
            iterate(os, newState, Set(o) +: parSeq)
          }
        case _ => Some(parSeq.reverse)
      }
    }

    //Method starts
    if (opsInStraightSeq.isEmpty) {
      None
    } else {
      iterate(opsInStraightSeq.tail, initState, Seq(Set(opsInStraightSeq.head)))
    }
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