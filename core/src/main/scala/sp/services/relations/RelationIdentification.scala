package sp.services.relations

import akka.actor._
import sp.domain.logic.IDAbleLogic
import sp.services.sopmaker.MakeASop
import scala.concurrent._
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._

import scala.annotation.tailrec

//TODO: future: background and foreground services. background takes hierarchy as input, foreground takes List, set or hierarchy
object RelationIdentification extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Find relations" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> SPAttributes("iterations" -> KeyDefinition("Int", List(1, 10, 100, 400, 1000, 2000), Some(100)),
      "operationIds" -> KeyDefinition("List[ID]", List(), Some(SPValue(List())))))

  val transformTuple = TransformValue("setup", _.getAs[RelationIdentificationSetup]("setup"))

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
class RelationIdentification extends Actor with ServiceSupport with RelationIdentificationLogic with DESModelingSupport {

  def receive = {
    case r@Request(service, attr, ids, reqID) =>
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      println(s"service: $service")

      // include this if you whant to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)
      progress ! SPAttributes("progress" -> "Intro")
      val setup = transform(RelationIdentification.transformTuple)

      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation]).toSet
      val vars = ids.filter(_.isInstanceOf[Thing]).map(_.asInstanceOf[Thing]).toSet

      //      println("ops size " + ops.size)

      def mapOps[T](t: T) = ops.map(o => o.id -> t).toMap
      //      val iState = State(mapOps(OperationState.init))
      val iState = State(getIdleState(vars).state ++ mapOps(OperationState.init))

      //      println("iState " + iState.state.mkString("\n"))

      def isGoalStateOps(state: State) = {
        lazy val goalState = State(mapOps(OperationState.finished))
        state.state.forall {
          case (id, value) =>
            goalState.get(id) match {
              case Some(otherValue) => value == otherValue
              case _ => false
            }
        }
      }

      def isGoalState(state: State) = {
        val a = stateIsMarked(vars)(state.copy(state = state.state.filterKeys(vars.map(_.id).contains)))
        val b = isGoalStateOps(state.copy(state = state.state.filterKeys(ops.map(_.id).contains)))
        //        println("ab " + a + b)
        a && b
      }
      val evaluateProp2 = EvaluateProp(mapOps((_: SPValue) => true), Set(), TwoStateDefinition)
      val evaluateProp3 = EvaluateProp(mapOps((_: SPValue) => true), Set(), ThreeStateDefinition)

      progress ! SPAttributes("progress" -> "Analyse operations")
      val (opSeqs, states) = analyseOperations(ops, setup.iterations, iState, { case (state, seq) => isGoalState(state) }, evaluateProp2)
      println("opSeqs " + opSeqs.size)

      progress ! SPAttributes("progress" -> "Create item relations")
      val itemRelationMap = createItemRelationsfrom(ops, states, evaluateProp2)
      //      println("itemRelationMap " + itemRelationMap.toList.map(kv => kv._1.name -> kv._2).mkString("\n"))

      progress ! SPAttributes("progress" -> "Create relation maps")
      val operationRelations = buildRelationMap(itemRelationMap, ops, opSeqs, iState, evaluateProp2, evaluateProp3)
      //      println("operationRelations " + operationRelations.mkString("\n"))

      progress ! SPAttributes("progress" -> "Create SOPs")
      val activeOps = if (setup.operationIds.isEmpty) ops else ops.filter(o => setup.operationIds.contains(o.id))
      import scala.util.Random
      val startOpsOps = if (activeOps.size > 3) Random.shuffle(activeOps).take(3) else activeOps
      val sops = startOpsOps.toList.flatMap(startOp => CreateSop(activeOps, operationRelations, startOp).createSop()).distinct
      if (sops.nonEmpty) {
        val toReturn = SOPSpec(name = "relationSOPs", sop = sops)
        //        rnr.reply ! Response(List(toReturn) ++ newOldMap.keys.toList, SPAttributes("info" -> s"created a relationMap for ${ops.map(_.name).mkString(" ")}"), service, reqID)
        rnr.reply ! Response(List(toReturn), SPAttributes("info" -> s"created a relationMap for ${ops.map(_.name).mkString(" ")}"), service, reqID)
      } else {
        rnr.reply ! Response(List(), SPAttributes(), service, reqID)
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

case class ItemRelation(id: ID, state: SPValue, relations: Map[ID, Set[SPValue]])

case class OperationRelation(opPair: Set[Operation], relation: Set[SOP]) {
  override def toString() = opPair.map(_.name).mkString("", " ", "\n") + relation.mkString("\n")
}

trait PostProcessSOP {

  def returnDuplicates(sop: SOP, newOldOpMap: Map[ID, ID]): SOP = {
    val sopList = sop.sop.foldLeft(Seq(): Seq[SOP]) {
      case (acc, s) =>
        println("s " + s)
        s.isInstanceOf[Hierarchy] match {
          case true =>
            val h = s.asInstanceOf[Hierarchy]
            val id = h.operation
            newOldOpMap.get(id) match {
              case Some(oldId) => acc ++ Seq(Hierarchy(oldId))
              case _ => acc ++ Seq(s)
            }
          case false =>
            acc ++ Seq(returnDuplicates(s, newOldOpMap))
        }
    }
    SOP(sopList: _*)
  }
}

trait RelationIdentificationLogic {

  def analyseOperations(ops: Set[Operation], iterations: Int, iState: State, goalCondition: (State, Seq[Operation]) => Boolean, evalSetup: EvaluateProp): (Set[Seq[Operation]], Set[State]) = {
    val (seqs, states, _) = (1 to iterations).foldLeft((Set(), Set(), Map()): (Set[Seq[Operation]], Set[State], Map[State, Set[Operation]])) {
      case (acc@(accOpseqs, accStates, accDeadLockStateOpMap), _) =>
        findStraightSeq(ops, iState, goalCondition, evalSetup, accDeadLockStateOpMap) match {
          case Some((opSeq, newStates, deadLockStateOpMap)) => (accOpseqs + opSeq, accStates ++ newStates, deadLockStateOpMap)
          case _ => acc
        }
    }
    (seqs, states)
  }

  def findStraightSeq(ops: Set[Operation], initState: State, goalCondition: (State, Seq[Operation]) => Boolean, evalSetup: EvaluateProp, deadLockStateOpMap: Map[State, Set[Operation]]): Option[(Seq[Operation], Set[State], Map[State, Set[Operation]])] = {
    implicit val es = evalSetup

    def getEnabledOperations(state: State) = {
      ops.filter(_.eval(state))
    }

    @tailrec
    def iterate(currentState: State, path: Seq[(State, Operation)], ioDeadLockStateOpMap: Map[State, Set[Operation]], backtrackingRandomReturnLengthOpt: Option[Int] = None): Option[(Seq[Operation], Set[State], Map[State, Set[Operation]])] = {
      import scala.util.Random
      goalCondition(currentState, path.unzip._2) match {
        case true =>
          val pathUnziped = path.unzip
          Some(pathUnziped._2.reverse, pathUnziped._1.toSet + currentState, ioDeadLockStateOpMap)
        case false =>
          //Get ops enabled from current state
          val enabledOps = {
            val ops = getEnabledOperations(currentState)
            ioDeadLockStateOpMap.get(currentState) match {
              case Some(badOps) => ops.intersect(badOps)
              case _ => ops
            }
          }
          if (enabledOps.isEmpty) {
            // If backtracking selected.
            // 1) Backtrack to a previous state in path
            // 2) Add path head to deadLockStateOpMap
            backtrackingRandomReturnLengthOpt match {
              case Some(backtrackingRandomReturnLength) =>
                val returnedPath = path.drop(Set(Random.nextInt(backtrackingRandomReturnLength + 1), path.size - 1).min)
                returnedPath.headOption match {
                  case Some((rState, _)) =>
                    val deadLockStateOpMapUpdOpt = path.headOption.map { case (s, op) => ioDeadLockStateOpMap + (s -> (ioDeadLockStateOpMap.getOrElse(s, Set()) + op)) }
                    iterate(rState, returnedPath.tail, deadLockStateOpMapUpdOpt.getOrElse(ioDeadLockStateOpMap))
                  case _ => None // returnedPath is empty => currentState is initial state, and enabledOps is empty. Thus no path can be found.
                }
              case _ => None
            }
          } else {
            val selectedOp = Random.shuffle(enabledOps).head
            iterate(selectedOp.next(currentState), (currentState, selectedOp) +: path, ioDeadLockStateOpMap)
          }
      }
    }
    iterate(initState, Seq(), deadLockStateOpMap)
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

    implicit val es3 = evalSetup3
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

    val opi = Set(OperationState.init)
    val opf = Set(OperationState.finished)
    val opif = opi ++ opf

    val arbiPairs = opSequences.flatMap(opSeq => arbitraryFinder(opSeq, iState, evalSetup2, evalSetup3))
    //    val opMap = activeOps.map(o => o.id -> o.name).toMap
    //    println(s"arbiPairs: ${arbiPairs.map(ops => ops.map(opMap))}")

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

trait DESModelingSupport {
  def getIdleState(vars: Set[Thing]) = {
    val state = vars.foldLeft(Map(): Map[ID, SPValue]) { case (acc, v) =>
      val optDomain = v.attributes.findField(f => f._1 == "domain").flatMap(_._2.to[List[String]])
      val allInitString = Set("init", "idleValue").flatMap(key => v.attributes.findAs[String](key))
      if (allInitString.size == 1) {
        optDomain match {
          case Some(domain) if domain.contains(allInitString.head) => acc + (v.id -> SPValue(domain.indexOf(allInitString.head)))
          case _ => acc
        }
      } else {
        val allInitInt = Set("init", "idleValue").flatMap(key => v.attributes.findAs[Int](key))
        if (allInitInt.size == 1) {
          acc + (v.id -> SPValue(allInitInt.head))
        } else {
          println(s"Problem with variable ${v.name}, attribute keys init and idleValue do not point to the same value")
          acc
        }
      }
    }
    State(state)
  }

  def stateIsMarked(vars: Set[Thing]): State => Boolean = { thatState =>
    val goalState = vars.flatMap { v =>
      val idleValueAttr = v.attributes.getAs[String] ("idleValue").map(Set(_))
      val markingsAttr = v.attributes.getAs[Set[String]] ("markings")
      val allMarkings = Set(idleValueAttr, markingsAttr).flatten
      if (allMarkings.size == 1) {
        val optDomain = v.attributes.findField(f => f._1 == "domain").flatMap(_._2.to[List[String]])
        optDomain match {
          case Some(domain) if (allMarkings.head -- domain.toSet).isEmpty => Some(v.id -> allMarkings.head.map(m => SPValue(domain.indexOf(m))))
          case _ =>
            println(s"Problem with variable ${v.name}, attribute keys markings/idleValue not in variable domain")
            None
        }
      } else {
        println(s"Problem with variable ${v.name}, attribute keys markings and idleValue do not point to the same value(s)")
        None
      }
    }.toMap

    import org.json4s.JsonAST.JInt
    def checkState(stateToCheck: Seq[(ID, SPValue)] = thatState.state.toSeq): Boolean = stateToCheck match {
      case kv +: rest => goalState.get(kv._1) match {
        case Some(marked) => kv._2 match {
          case v@JInt(_) => if (marked.contains(v)) checkState(rest) else false
          case _ => false
        }
        case _ => false //"stateToCheck" contains variables that is not in "goalState". This should although not happen...
      }
      case _ => true //"stateToCheck" == "goalState"
    }
    checkState()
  }
}

