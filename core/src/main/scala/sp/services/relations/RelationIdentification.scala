package sp.services.relations

import akka.actor._
import akka.pattern.ask
import sp.domain.logic.IDAbleLogic
import sp.services.sopmaker.{MakeASop, MakeMeASOP, SOPMaker}
import scala.concurrent._
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._

import scala.annotation.tailrec
import scala.util.{Failure, Success}

//TODO: future: background and foreground services. background takes hierarchy as input, foreground takes List, set or hierarchy
object RelationIdentification extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Find relations" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> SPAttributes("iterations" -> KeyDefinition("Int", List(), Some(10)),
      "operationIds" -> KeyDefinition("List[ID]", List(), Some(SPValue(List())))))

  val transformTuple =
    TransformValue("setup", _.getAs[RelationIdentificationSetup]("setup"))

  val transformation = transformToList(transformTuple.productIterator.toList)

  // important to incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[RelationIdentification]))

  // Alla får även "core" -> ServiceHandlerAttributes

  //  case class ServiceHandlerAttributes(model: Option[ID],
  //                                      responseToModel: Boolean,
  //                                      onlyResponse: Boolean,
  //                                      includeIDAbles: List[ID])

}

case class RelationIdentificationSetup(iterations: Int, operationIds: List[ID])

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class RelationIdentification extends Actor with ServiceSupport with RelationIdentificationLogic {

  def receive = {
    case r@Request(service, attr, ids, reqID) =>
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you whant to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      val setup = transform(RelationIdentification.transformTuple)

      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation]).toSet

      def mapOps[T](t: T) = ops.map(o => o.id -> t).toMap
      val iState = State(mapOps(OperationState.init))
      def isGoalState(state: State) = {
        lazy val goalState = State(mapOps(OperationState.finished))
        state.state.forall {
          case (id, value) =>
            goalState.get(id) match {
              case Some(otherValue) => value == otherValue
              case _ => false
            }
        }
      }
      val evaluateProp2 = EvaluateProp(mapOps((_: SPValue) => true), Set(), TwoStateDefinition)
      val evaluateProp3 = EvaluateProp(mapOps((_: SPValue) => true), Set(), ThreeStateDefinition)

      val (opSeqs, states) = analyseOperations(ops, setup.iterations, iState, { case (state, seq) => isGoalState(state) }, evaluateProp2)
      val itemRelationMap = createItemRelationsfrom(ops, states, evaluateProp2)
      val operationRelations = buildRelationMap(itemRelationMap, ops, opSeqs, iState, evaluateProp2, evaluateProp3)
      val activeOps = if (setup.operationIds.isEmpty) ops else ops.filter(o => setup.operationIds.contains(o.id))

      val sops = CreateSop(activeOps, operationRelations, ops.head).createSop()

      val toReturn = SOPSpec(name = "relationSOP", sop = sops)
      rnr.reply ! Response(List(toReturn), SPAttributes("info" -> s"created a relationMap for ${ops.map(_.name).mkString(" ")}"), service, reqID)

      progress ! PoisonPill
      self ! PoisonPill

    case (r: Response, reply: ActorRef) =>
      reply ! r
    case x =>
      sender() ! SPError("What do you want me to do? " + x)
      self ! PoisonPill
  }

}

case class ItemRelation(id: ID, state: SPValue, relations: Map[ID, Set[SPValue]])

case class OperationRelation(opPair: Set[Operation], relation: Set[SOP])

trait RelationIdentificationLogic {

  def analyseOperations(ops: Set[Operation], iterations: Int, iState: State, goalCondition: (State, Seq[Operation]) => Boolean, evalSetup: EvaluateProp): (Set[Seq[Operation]], Set[State]) = {
    (0 to iterations).foldLeft((Set(), Set()): (Set[Seq[Operation]], Set[State])) {
      case (acc@(accOpseqs, accStates), _) =>
        findStraightSeq(ops, iState, goalCondition, evalSetup) match {
          case Some((opSeq, newStates)) => (accOpseqs + opSeq, accStates ++ newStates)
          case _ => acc
        }
    }
  }

  def findStraightSeq(ops: Set[Operation], initState: State, goalCondition: (State, Seq[Operation]) => Boolean, evalSetup: EvaluateProp): Option[(Seq[Operation], Set[State])] = {
    implicit val es = evalSetup
    def getEnabledOperations(state: State) = ops.filter(_.eval(state))

    @tailrec
    def iterate(currentState: State, opSeq: Seq[Operation] = Seq(), states: Set[State] = Set()): Option[(Seq[Operation], Set[State])] = {
      goalCondition(currentState, opSeq) match {
        case true => Some(opSeq.reverse, states + currentState)
        case false =>
          lazy val enabledOps = getEnabledOperations(currentState)
          if (enabledOps.isEmpty) {
            None
          } else {
            import scala.util.Random
            lazy val selectedOp = Random.shuffle(enabledOps).head
            iterate(selectedOp.next(currentState), selectedOp +: opSeq, states + currentState)
          }
      }
    }
    iterate(initState)
  }
  def createItemRelationsfrom(ops: Set[Operation], states: Set[State], evalSetup: EvaluateProp): Map[Operation, ItemRelation] = {

    implicit val es = evalSetup
    def getEnabledOperations(state: State) = ops.filter(_.eval(state))
    val itemRelMap = states.foldLeft(Map(): Map[Operation, ItemRelation]) {
      case (acc, s) =>
        val enabledOps = getEnabledOperations(s)
        val res = enabledOps.map { o =>
          val ir = acc.getOrElse(o, ItemRelation(o.id, OperationState.init, Map()))

          val newMap = s.state.map {
            case (id, value) =>
              id -> (ir.relations.getOrElse(id, Set()) + value)
          }
          o -> ir.copy(relations = newMap)
        }
        acc ++ res
    }
    itemRelMap
  }

  def arbitraryFinder(sequence: Seq[Operation], iState: State, evalSetup2: EvaluateProp, evalSetup3: EvaluateProp) = {

    @tailrec
    def findNotPar(s: State, seq: Seq[Operation]): Set[Operation] = {
      implicit val es2 = evalSetup2
      if (seq.isEmpty) Set()
      else if (!seq.head.eval(s)) Set(seq.head)
      else {
        findNotPar(seq.head.next(s), seq.tail)
      }
    }

    implicit val es2 = evalSetup3
    @tailrec
    def req(s: State, seq: Seq[Operation], res: Set[Set[ID]]): Set[Set[ID]] = {
      if (seq.isEmpty) res
      else {
        val o = seq.head
        if (!o.eval(s)) println("SOMETHING IS WRONG IN OVERLAP FINDER OP " + o + s)
        val newState = o.next(s)
        val parallelTo = findNotPar(newState, seq.tail).map(x => Set(o.id, x.id))
        req(o.next(newState), seq.tail, res ++ parallelTo)
      }
    }
    req(iState, sequence, Set())
  }

  def buildRelationMap(itemRelations: Map[Operation, ItemRelation], activeOps: Set[Operation], opSequences: Set[Seq[Operation]], iState: State, evalSetup2: EvaluateProp, evalSetup3: EvaluateProp): Set[OperationRelation] = {
    //TODO: create Parallel, Arbitary orders and return
    val opi = Set(OperationState.init)
    val opf = Set(OperationState.finished)
    val opif = opi ++ opf

    val arbiPairs = opSequences.flatMap(opSeq => arbitraryFinder(opSeq, iState, evalSetup2, evalSetup3))

    def matchOps(o1: ID, valuesOfO2WhenO1Enabled: Set[SPValue], o2: ID, valuesOfO1WhenO2Enabled: Set[SPValue]): SOP = {
      val pre = (valuesOfO2WhenO1Enabled, valuesOfO1WhenO2Enabled)
      if (pre ==(opi, opi)) Alternative(o1, o2)
      else if (pre ==(opi, opf)) Sequence(o1, o2)
      else if (pre ==(opf, opi)) Sequence(o2, o1)
      else if (pre ==(opif, opi)) Sequence(o2, o1) //SometimeSequence(o2, o1)
      else if (pre ==(opi, opif)) Sequence(o1, o2) //SometimeSequence(o1, o2)
      else if (pre ==(opif, opif)) if (arbiPairs.contains(Set(o1, o2))) Arbitrary(o1, o2) else Parallel(o1, o2)
      else Other(o1, o2)
    }

    def getRelation(thisOp: Operation, thatOp: Operation): Set[SPValue] = {
      itemRelations.get(thisOp) match {
        case Some(ir) => ir.relations.getOrElse(thatOp.id, Set())
        case _ => Set()
      }
    }

    @tailrec
    def iterate(operations: Seq[Operation], opRels: Set[OperationRelation] = Set()): Set[OperationRelation] = {
      operations match {
        case thisOp +: os if os.nonEmpty =>
          val res = os.map { thatOp =>
            val sop = matchOps(thisOp.id, getRelation(thisOp, thatOp), thatOp.id, getRelation(thatOp, thisOp))
            OperationRelation(Set(thisOp, thatOp), Set(sop))
          }
          iterate(os, opRels ++ res)
        case _ => opRels
      }
    }
    iterate(activeOps.toSeq)

  }

  case class CreateSop(activeOps: Set[Operation], operationRelations: Set[OperationRelation], rootOp: Operation) extends MakeASop {
    val relations = operationRelations.map(or => or.opPair.map(_.id) -> or.relation.headOption.getOrElse(EmptySOP)).toMap
    def createSop() = makeTheSop(activeOps.map(_.id).toList, relations, if (activeOps.contains(rootOp)) SOP.apply(rootOp) else EmptySOP)
  }

}

