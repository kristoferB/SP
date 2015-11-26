package sp.virtcom

import akka.actor.{PoisonPill, Props, Actor, ActorRef}
import sp.domain._
import sp.supremicaStuff.auxiliary.DESModelingSupport
import sp.system.{ServiceSupport, ServiceLauncher, SPService}
import sp.system.messages._
import sp.domain.Logic._

/**
 * Given a straight operation sequence as a specification, this actor uses breadth-first search to find
 * a straight operation sequence from the initial state (i) to a marked state (m) such that the operations in the
 * specification can be executed in the order specified.
 *
 * Given the straight operation sequence:
 * op1 -> op2
 * this actor will return:
 * (i) -> Seq[Operation] -> op1 -> Seq[Operation] -> op2 -> Seq[Operation] -> (m)
 *
 * The actor is greedy such that each Seq[Operation] contains the fewest number of operations possible.
 * The sequence can thus be empty.
 *
 * The found operation sequence is returned to the model as a SOPSpec with a single Sequence that references
 * to the corresponding operations in the model.
 * The returned SOPSpec has the same name as the specification prefixed with "_Result".
 *
 * The actor will try to find an operation sequence for all SOPSpecs that are checked when the actor is called.
 * No SOPSpec is returned to the model for a checked SOPSpec if:
 * 1) the SOPSpec does not contain a straight operation sequence
 * 2) no operation sequence is found
 *
 * @author Patrik Bergagard
 */

object CreateInstanceModelFromTypeModelService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Create one instance form type models"
    ),
    "Specifications" -> SPAttributes(
      "sops" -> KeyDefinition("List[ID]", List(), Some(SPValue(List()))),
      "generateIdables" -> KeyDefinition("Boolean", List(), Some(false))
    )
  )

  val transformTuple = (
    TransformValue("Specifications", _.getAs[CreateInstanceModelFromTypeModelSpecifications]("Specifications"))
  )

  val transformation = transformToList(transformTuple.productIterator.toList)

  def props = ServiceLauncher.props(Props(classOf[CreateInstanceModelFromTypeModelService]))

}

case class CreateInstanceModelFromTypeModelSpecifications(sops: List[ID], generateIdables: Boolean)

class CreateInstanceModelFromTypeModelService extends Actor with ServiceSupport with DESModelingSupport with CopyModifyAndAddIdables with AddHierarchies {

  def receive = {
    case r@Request(service, attr, ids, reqID) =>

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val progress = context.actorOf(progressHandler)

      println(s"service: $service")

      lazy val specifications = transform(CreateInstanceModelFromTypeModelService.transformTuple)

      lazy val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
      lazy val vars = ids.filter(_.isInstanceOf[Thing]).map(_.asInstanceOf[Thing])
      lazy val specs = ids.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec]).filter(sop => specifications.sops.contains(sop.id))

      println(s"specs: ${specs.map(_.name)}")

      lazy val specList = {
        val onlySopsWithSequences = specs.flatMap { spec =>
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
        transformToOps.filter(_._2.nonEmpty)
      }


      lazy val initState = getIdleState(vars.toSet)

      lazy val markedState = stateIsMarked(vars.toSet)

      import sp.domain.logic.PropositionConditionLogic._

      if (specList.isEmpty) println("No specification(s) given.")

      case class SequenceResult(startSeqName: String, oldOpSeq: Seq[Operation], opSeq: Seq[Operation], result: SOPSpec)

      if (ops.exists(_.conditions.size != 2)) {
        println("At least one operation lacks pre and/or postcondition. I will not go on.")
        rnr.reply ! Response(List(), SPAttributes(), service, reqID)
        self ! PoisonPill
      } else {
        val result = specList.flatMap { case (name, opSeqFromSpec) =>

          val wfs = WrapperForSearch(ops.toSet)

          def findOpSeqIterator(remainingOpList: Seq[Operation], startState: State = initState, opSeqToReturn: Seq[Operation] = Seq()): Option[Seq[Operation]] = remainingOpList match {
            //No more operations remains in list
            case Nil => wfs.findOpSeq(markedState, Set(startState), Map(startState -> Seq())) match {
              case Some(wfs.OpSeqResult(_, opSeq)) =>
                val finalOpSeq = opSeq ++ opSeqToReturn
                Some(finalOpSeq.reverse)
              case _ => None
            }
            //At least one more operation remains in list
            //The goal state is set to the first state from where the first operation in the list of remaining operations can start
            case o :: os => wfs.findOpSeq(o.conditions.head.eval, Set(startState), Map(startState -> Seq())) match {
              case Some(wfs.OpSeqResult(lastState, opSeq)) =>
                val nextStartState = wfs.updateStateBasedOnAllOperationActions(lastState, o)
                findOpSeqIterator(os, nextStartState, Seq(o) ++ opSeq ++ opSeqToReturn)
              case _ => None
            }

          }

          println("search started")
          progress ! SPAttributes("progress" -> "search started")
          val foundSeq = findOpSeqIterator(opSeqFromSpec) match {
            case Some(seq) =>
              println(s"The found sequence for specification \'$name\' contains ${seq.size} operations.")
              Some((seq, Sequence(seq.map(o => Hierarchy(o.id, List())): _*)))
            case _ =>
              println(s"No sequence for specification \'$name\' could be found.")
              None
          }
          println("search ended")
          progress ! SPAttributes("progress" -> "search ended")

          foundSeq.map { case (opSeq, sequence) =>
            SequenceResult(name, opSeqFromSpec, opSeq, SOPSpec(name = s"${name}_Result", sop = List(sequence), attributes = SPAttributes().addTimeStamp))
          }

        }

        if (result.nonEmpty) {
          if(specifications.generateIdables) {
            val (newIdablesList, newOldOpMapList) = result.map(r => copyModifyAndAddIdables(r.startSeqName, r.oldOpSeq, r.opSeq, vars.toSet)).unzip
            val newIdables = newIdablesList.foldLeft(Set(): Set[IDAble]) { case (acc, newIds) => acc ++ newIds }.toList
            val newIdablesWithHierarchies = newIdables ++ addHierarchies(newIdables, "hierarchy")

            rnr.reply ! Response(newIdablesWithHierarchies, SPAttributes(), service, reqID)
          } else {
            rnr.reply ! Response(result.map(_.result), SPAttributes("info" -> s"Operation sequence(s) created from specification(s): ${result.map(_.startSeqName).mkString(", ")}"), service, reqID)
          }
        } else {
          rnr.reply ! Response(List(), SPAttributes(), service, reqID)
        }

        progress ! PoisonPill
        self ! PoisonPill
      }

    case (r: Response, reply: ActorRef) =>
      reply ! r
    case x =>
      sender() ! SPError("What do you whant me to do? " + x)
      self ! PoisonPill
  }
}

case class WrapperForSearch(ops: Set[Operation]) {

  import sp.domain.logic.PropositionConditionLogic._

  case class OpSeqResult(finalState: State, opSeq: Seq[Operation])

  def updateStateBasedOnAllOperationActions(startState: State, o: Operation) = o.conditions.foldLeft(startState: State) { case (acc, c) => c.next(acc) }

  def findOpSeq(terminationCondition: State => Boolean, freshStates: Set[State], visitedStates: Map[State, Seq[Operation]]): Option[OpSeqResult] = {

    def terminate(fStates: Seq[State] = freshStates.toSeq): Option[OpSeqResult] = fStates match {
      case s +: ss => if (terminationCondition(s)) Some(OpSeqResult(s, visitedStates(s))) else terminate(ss)
      case _ => None
    }

    case class LocalResult(fStates: Set[State], vStates: Map[State, Seq[Operation]])
    def oneMoreIteration() = {
      val enabledOpsFromEachFreshState = freshStates.map(s => s -> ops.filter(_.conditions(0).eval(s)))
      val newStates = enabledOpsFromEachFreshState.foldLeft(new LocalResult(Set(), visitedStates): LocalResult) { case (acc, (s, os)) =>
        val newAcc = os.foldLeft(acc: LocalResult) { case (accInner, o) =>
          val targetStateForOperationO = updateStateBasedOnAllOperationActions(s, o)
          if (accInner.vStates.contains(targetStateForOperationO))
          //Target state has been visited before. Take no action.
            accInner.copy()
          else
          //Target state has not been visited before. Add to accumulator
            LocalResult(accInner.fStates + targetStateForOperationO, Map(targetStateForOperationO -> (o +: accInner.vStates(s))) ++ accInner.vStates)
        }
        newAcc
      }
      //Inner method starts
      findOpSeq(terminationCondition, newStates.fStates, newStates.vStates)
    }

    //Method starts
    if (freshStates.isEmpty) None
    else {
      terminate() match {
        case None => oneMoreIteration()
        case result => result
      }
    }

  }

}

trait CopyModifyAndAddIdables extends CollectorModel {
  val modelName = ""
  def copyModifyAndAddIdables(seqName: String, oldStraightOpSeq: Seq[Operation], straightOpSeq: Seq[Operation], existingVars: Set[Thing]): (Set[IDAble], Map[Operation, Operation]) = {
    val hAtt = SPAttributes("hierarchy" -> Set(seqName))

    type copyAndModifyOpsReturnType = (Map[Operation, Operation], Map[Operation, Int])
    def copyAndModifyOps(oldOpSeq: Seq[Operation], oldSpec: Seq[(Operation, Operation)], newOldOpMap: Map[Operation, Operation], oldOpFreqMap: Map[Operation, Int]): copyAndModifyOpsReturnType = oldOpSeq match {
      case o +: os =>
        val vName = s"v${o.name.capitalize}"
        val freq = oldOpFreqMap.getOrElse(o, 0)
        val (orderGuard, oldSpecUpd) = oldSpec.headOption match {
          case Some((oAfter, oBefore)) if oAfter.equals(o) =>
            oldOpFreqMap.get(oBefore) match {
              case Some(f) => (SPAttributes("preGuard" -> Set(s"v${oBefore.name.capitalize}==$f")), oldSpec.tail)
              case _ =>
                println(s"Problem when adding guards for operation orders. ${oBefore.name} does not come before ${oAfter.name}, as in spec. Result will not be ok.")
                (SPAttributes(), oldSpec.tail)
            }
          case _ => (SPAttributes(), oldSpec)
        }
        val attr = SPAttributes("preGuard" -> Set(s"$vName==$freq")) merge orderGuard merge SPAttributes("postGuard" -> Set(s"$vName==$freq")) merge SPAttributes("postAction" -> Set(s"$vName=${freq + 1}"))
        val oldAttr = (o.attributes transformField { case ("hierarchy", _) => ("hierarchy", SPValue(Set(seqName))) }).to[SPAttributes].getOrElse(SPAttributes())
        val newOp = o.copy(name = s"$vName$freq", id = ID.newID, attributes = oldAttr merge attr merge hAtt)
        copyAndModifyOps(os, oldSpecUpd, newOldOpMap + (newOp -> o), oldOpFreqMap + (o -> (freq + 1)))
      case _ => (newOldOpMap, oldOpFreqMap)
    }

    def addVarsForOps(opFreqMap: Map[Operation, Int]): Set[Thing] = {
      opFreqMap.keys.foreach { o =>
        val vName = s"v${o.name.capitalize}"
        val last = opFreqMap(o)
        val domain = (0 to last).map(_.toString)
        v(vName, domain, init = Some("0"), marked = Set(last.toString), attributes = hAtt)
      }
      variableSet
    }

    def addHierarchyToExistingVars(eVars: Set[Thing]) = eVars.map(v => v.copy(attributes = v.attributes merge hAtt))

    def workOnInitialSpec(spec: Seq[Operation], acc: Seq[(Operation, Operation)] = Seq()) : Seq[(Operation, Operation)] = spec match {
      case o1 +: rest if rest.nonEmpty => workOnInitialSpec(rest,(rest.head, o1) +: acc)
      case _ => acc.reverse
    }

    val (newOldOpMap, oldOpFreqMap) = copyAndModifyOps(straightOpSeq, workOnInitialSpec(oldStraightOpSeq), Map(), Map())
    val vars = addVarsForOps(oldOpFreqMap) ++ addHierarchyToExistingVars(existingVars)
    (newOldOpMap.keySet ++ vars, newOldOpMap)
  }
}
