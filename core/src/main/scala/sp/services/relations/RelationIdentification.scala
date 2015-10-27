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

object RelationIdentification extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Find relations" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> SPAttributes())

  val transformTuple = (
    TransformValue("setup", _.getAs[RelationIdentification]("setup")))

  val transformation = transformToList(transformTuple.productIterator.toList)

  // important to incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[RelationIdentification]))

  // Alla får även "core" -> ServiceHandlerAttributes

  //  case class ServiceHandlerAttributes(model: Option[ID],
  //                                      responseToModel: Boolean,
  //                                      onlyResponse: Boolean,
  //                                      includeIDAbles: List[ID])

}

case class RelationIdentificationSetup(onlyOperations: Boolean, searchMethod: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class RelationIdentification extends Actor with ServiceSupport with RelationIdentificationLogic {

  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) =>

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you whant to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      //val s = transform(RelationMapper.transformTuple._1)

      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation]).toSet

      def mapOps[T](t: T) = ops.map(o => o.id -> t).toMap
      val iState = State(mapOps(OperationState.init))
      def isGoalState(state: State) = {
        lazy val goalState = State(mapOps(OperationState.finished))
        state.state.forall { case (id, value) =>
          goalState.get(id) match {
            case Some(otherValue) => value == otherValue
            case _ => false
          }
        }
      }
      val evalualteProp2 = EvaluateProp(mapOps((_: SPValue) => true), Set(), TwoStateDefinition)

      val (opSeqs, states) = (0 to 10).foldLeft((Set(), Set()): (Set[Seq[Operation]], Set[State])) {
        case (acc@(accOpseqs, accStates), _) =>
          findStraightSeq(ops, iState, evalualteProp2, { case (state, seq) => isGoalState(state) }) match {
            case Some((opSeq, newStates)) => (accOpseqs + opSeq, accStates ++ newStates)
            case _ => acc
          }
      }
      val itemRelationMap = createItemRelationsfrom(ops, states, evalualteProp2)
      val operationRelations = buildRelationMap(itemRelationMap, ops, opSeqs)

      val sops = CreateSop(ops, operationRelations, ops.head).createSop()

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

//case class SeqState(op: Option[Operation] = None, state: State)
case class ItemRelation(id: ID, state: SPValue, relations: Map[ID, Set[SPValue]])

case class OperationRelation(opPair: Set[Operation], relation: Set[SOP])

//opPair just has always has two operations

sealed trait RelationIdentificationLogic {

  def findStraightSeq(ops: Set[Operation], initState: State, evalSetup: EvaluateProp, goalCondition: (State, Seq[Operation]) => Boolean): Option[(Seq[Operation], Set[State])] = {
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

  def buildRelationMap(itemRelations: Map[Operation, ItemRelation], activeOps: Set[Operation], opSequences: Set[Seq[Operation]]): Set[OperationRelation] = {

    val opi = Set(OperationState.init)
    val opf = Set(OperationState.finished)
    val opif = opi ++ opf
    def matchOps(o1: ID, valuesOfO2WhenO1Enabled: Set[SPValue], o2: ID, valuesOfO1WhenO2Enabled: Set[SPValue]): SOP = {

      val pre = (valuesOfO2WhenO1Enabled, valuesOfO1WhenO2Enabled)
      if (pre ==(opi, opi)) Alternative(o1, o2)
      else if (pre ==(opi, opf)) Sequence(o1, o2)
      else if (pre ==(opf, opi)) Sequence(o2, o1)
      else if (pre ==(opif, opi)) Sequence(o2, o1) //SometimeSequence(o2, o1)
      else if (pre ==(opi, opif)) Sequence(o1, o2) //SometimeSequence(o1, o2)
      else if (pre ==(opif, opif)) Parallel(o1, o2)
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

