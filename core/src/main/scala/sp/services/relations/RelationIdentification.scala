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
    "setup" -> SPAttributes("iterations" -> KeyDefinition("Int", List(1, 10, 200, 600, 1000, 2000), Some(1000)),
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
class RelationIdentification extends Actor with ServiceSupport with RelationIdentificationLogic with DESModelingSupport with PreProcessSOP with PostProcessSOP {

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

      val ops_ = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation]).toSet
      val vars = ids.filter(_.isInstanceOf[Thing]).map(_.asInstanceOf[Thing]).toSet
      val sopSpecs = ids.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec]).toSet

      val straightOpSeqs = getStraightOPSeqs(sopSpecs, ops_)
      //      println("straightOpSeqs " + straightOpSeqs.map(kv => kv._2.map(_.name).mkString("\n")))
      val (straightOpSeqsUpd, newOldMap) = replaceDuplicates(straightOpSeqs)
      //      println("straightOpSeqsUpd " + straightOpSeqsUpd.map(kv => kv._2.map(_.name).mkString("\n")))
      //      println("newOldMap " + newOldMap.map(kv => kv._1.name -> kv._2.name).mkString("\n"))

      val ops = straightOpSeqsUpd.foldLeft(Set(): Set[Operation]) { case (acc, (_, os)) => acc ++ os }
      println("ops size " + ops.size)

      def mapOps[T](t: T) = ops.map(o => o.id -> t).toMap
      //      val iState = State(mapOps(OperationState.init))
      val iState = State(getIdleState(vars).state ++ mapOps(OperationState.init))
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
      println("opSeqs " + opSeqs.map(_.map(_.name)).mkString("\n"))

      progress ! SPAttributes("progress" -> "Create item relations")
      val itemRelationMap = createItemRelationsfrom(ops, states, evaluateProp2)
      //      println("itemRelationMap " + itemRelationMap.toList.map(kv => kv._1.name -> kv._2).mkString("\n"))

      progress ! SPAttributes("progress" -> "Create relation maps")
      val operationRelations = buildRelationMap(itemRelationMap, ops, opSeqs, iState, evaluateProp2, evaluateProp3)
      println("operationRelations " + operationRelations.mkString("\n"))

      progress ! SPAttributes("progress" -> "Create SOPs")
      val activeOps = if (setup.operationIds.isEmpty) ops else ops.filter(o => setup.operationIds.contains(o.id))
      import scala.util.Random
      val startOpsOps = if (activeOps.size>3) Random.shuffle(activeOps).take(3) else activeOps
      val sops = startOpsOps.toList.flatMap(startOp => CreateSop(activeOps, operationRelations, startOp).createSop())
      if (sops.nonEmpty) {
        val toReturn = SOPSpec(name = "relationSOPs", sop = sops)
        rnr.reply ! Response(List(toReturn)++newOldMap.keys.toList, SPAttributes("info" -> s"created a relationMap for ${ops.map(_.name).mkString(" ")}"), service, reqID)
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

trait PreProcessSOP {

  def getStraightOPSeqs(sopSpecs: Set[SOPSpec], ops: Set[Operation]): Map[String, Seq[Operation]] = {
    val onlySopsWithSequences = sopSpecs.flatMap { spec =>
      val sopsWithSeq = spec.sop.filter(_.isInstanceOf[Sequence])
      sopsWithSeq.map(seq => s"${spec.name}${if (sopsWithSeq.size > 1) spec.sop.indexOf(seq) else ""}" -> seq.sop)
    }
    val onlyHierarchy = onlySopsWithSequences.flatMap {
      case (name, seq) if seq.forall(_.isInstanceOf[Hierarchy]) => Some(name, seq.map(_.asInstanceOf[Hierarchy]).map(_.operation))
      case _ => None
    }
    lazy val opsIdMap = ops.map(o => o.id -> o).toMap
    val transformToOps = onlyHierarchy.flatMap { case (name, seq) => val optList = seq.map(id => opsIdMap.get(id))
      if (optList.forall(_.isDefined)) Some((name, optList.flatMap(o => o))) else None
    }
    transformToOps.filter(_._2.nonEmpty).toMap
  }

  def replaceDuplicates(straightOpSeqs: Map[String, Seq[Operation]]): (Map[String, Seq[Operation]], Map[Operation, Operation]) = {
    def replaceDuplicatesForOpSeq(oldOpSeq: Seq[Operation], newOldOpMap: Map[Operation, Operation], newOpSeq: Seq[Operation] = Seq(), lastOpMap: Map[Operation, Operation] = Map()): (Seq[Operation], Map[Operation, Operation]) = oldOpSeq match {
      case o +: os => if (newOpSeq.contains(o)) {
        val newOp: Operation = o.copy(name = s"${o.name}", id = ID.newID,
          attributes = o.attributes merge SPAttributes("after" -> lastOpMap(o).id))
//        val sopSeq = SOP(Sequence(lastOpMap(o), newOp))
//        val conditions = sp.domain.logic.SOPLogic.extractOperationConditions(List(sopSeq), "traj")
//        val cond = conditions(newOp.id).asInstanceOf[PropositionCondition]
//        val newOpPropCond = newOp.conditions.head.asInstanceOf[PropositionCondition]
//        val newOpPropCondUpd = newOpPropCond.copy(guard = AND(List(newOpPropCond.guard, cond.guard)))
//        val newOpUpd = newOp.copy(conditions = newOpPropCondUpd :: newOp.conditions.tail,
//          attributes = newOp.attributes merge SPAttributes("after" -> lastOpMap(o).id))
        replaceDuplicatesForOpSeq(os, newOldOpMap + (newOp -> o), newOp +: newOpSeq, lastOpMap + (o -> newOp))
      } else {
        replaceDuplicatesForOpSeq(os, newOldOpMap, o +: newOpSeq, lastOpMap + (o -> o))
      }
      case _ => (newOpSeq.reverse, newOldOpMap)
    }
    straightOpSeqs.foldLeft((Map(), Map()): (Map[String, Seq[Operation]], Map[Operation, Operation])) { case (acc, (name, seq)) =>
      val (newSeq, newOldMap) = replaceDuplicatesForOpSeq(seq, acc._2)
      (acc._1 + (name -> newSeq), newOldMap)
    }
  }
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
          acc ++ Seq(returnDuplicates(s,newOldOpMap))
      }
    }
    SOP(sopList: _*)
  }
}

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

    def getEnabledOperations(state: State) = {
      //      ops.filter(_.eval(state))
      val opsToReturn = ops.filter(_.eval(state))
      def propLogic() = {
        import sp.domain.logic.PropositionConditionLogic._
        opsToReturn.filter { o =>
          //          println("o " + o.name + " " + o.attributes)
          o.attributes.getAs[ID]("after") match {
            case Some(id) =>
              state.state.get(id) match {
                case Some(stateValue) if stateValue == OperationState.finished => true
                case _ => false
              }
            case _ => true
          }
        }
      }
      val opsToReturn2 = propLogic()
      //      println("enabledOps " + opsToReturn.map(_.name))
      //      println("enabledOps2 " + opsToReturn2.map(_.name))
      opsToReturn2
    }


    @tailrec
    def iterate(currentState: State, opSeq: Seq[Operation] = Seq(), states: Set[State] = Set()): Option[(Seq[Operation], Set[State])] = {
      //      println("currentState " + goalCondition(currentState, opSeq) + "\n" + currentState.state.mkString("\n"))
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
      lazy val optDomain = v.attributes.findField(f => f._1 == "domain").flatMap(_._2.to[List[String]])
      v.attributes.getAs[Int]("idleValue") match {
        //Do nothing if idleValue is an int
        case Some(value) => acc + (v.id -> SPValue(value))
        //If not an int try with a string
        case _ => v.attributes.getAs[String]("idleValue") match {
          case Some(value) => optDomain match {
            //replace string value with position in domain
            case Some(domain) if domain.contains(value) => acc + (v.id -> SPValue(domain.indexOf(value)))
            case _ => acc
          }
          case _ => acc
        }
      }
    }
    State(state)
  }

  def stateIsMarked(vars: Set[Thing]): State => Boolean = { thatState =>
    lazy val goalState = getIdleState(vars)
    def checkState(stateToCheck: Seq[(ID, SPValue)] = thatState.state.toSeq): Boolean = stateToCheck match {
      case kv +: rest => goalState.get(kv._1) match {
        case Some(v) => if (v.equals(kv._2)) checkState(rest) else false
        case _ => false //"stateToCheck" contains variables that is not in "goalState". This should although not happen...
      }
      case _ => true //"stateToCheck" == "goalState"
    }
    checkState()
  }
}

