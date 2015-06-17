package sp.virtcom

import akka.actor.{Props, Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import org.json4s.JsonAST.JBool
import sp.domain._
import sp.jsonImporter.ServiceSupportTrait
import sp.system.messages._
import scala.concurrent.duration._
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
class CreateInstanceModelFromTypeModelService(modelHandler: ActorRef) extends Actor with ServiceSupportTrait {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(service, attr) => {

      println(s"service: $service")

      val id = attr.getAs[ID]("activeModelID").getOrElse(ID.newID)
      val checkedItems = attr.findObjectsWithField(List(("checked", JBool(true)))).unzip._1.flatMap(ID.makeID)

      //Search for operation sequence.
      for {
        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))

        //Get operations and variables
        SPIDs(opsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetOperations(model = modelInfo.model))
        ops = opsToBe.map(_.asInstanceOf[Operation])
        SPIDs(varsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetThings(model = modelInfo.model))
        vars = varsToBe.map(_.asInstanceOf[Thing])

        //Get specification for the operation sequence to return
        SPIDs(spSpecToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetSpecs(model = modelInfo.model))
        specs = spSpecToBe.filter(obj => checkedItems.contains(obj.id)).filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec])

        specList = {
          val onlySopsWithSequences = specs.flatMap { spec =>
            val sopsWithSeq = spec.sop.filter(_.isInstanceOf[Sequence])
            sopsWithSeq.map(seq => s"${spec.name}${if (sopsWithSeq.size > 1) spec.sop.indexOf(seq) else ""}" -> seq.sop)
          }
          val onlyHierarchy = onlySopsWithSequences.flatMap {
            case (name, seq) if (seq.forall(_.isInstanceOf[Hierarchy])) => Some(name, seq.map(_.asInstanceOf[Hierarchy]).map(_.operation))
            case _ => None
          }
          lazy val opsIdMap = ops.map(o => o.id -> o).toMap
          val transformToOps = onlyHierarchy.flatMap { case (name, seq) => val optList = seq.map(id => opsIdMap.get(id))
            if (optList.forall(_.isDefined)) Some((name, optList.flatMap(o => o))) else None
          }
          transformToOps.filter(_._2.nonEmpty)
        }

      } yield {
        val initState = getInitState(vars.toSet)
        val markedState = stateIsMarked(vars.toSet)

        import sp.domain.logic.PropositionConditionLogic._

        if(specList.isEmpty) println("No specification(s) given.")

        specList.foreach { case (name, opSeqFromSpec) =>

          val wfs = WrapperForSearch(ops.toSet)

          def findOpSeqIterator(remainingOpList: Seq[Operation], startState: State = initState, opSeqToReturn: Seq[Operation] = Seq()): Option[Seq[Operation]] = remainingOpList match {
            case Nil => wfs.findOpSeq(markedState, Set(startState), Map(startState -> Seq())) match {
              case Some(wfs.OpSeqResult(_, opSeq)) =>
                val finalOpSeq = opSeq ++ opSeqToReturn
                Some(finalOpSeq.reverse)
              case _ => None
            }
            case o :: os => wfs.findOpSeq(o.conditions.head.eval, Set(startState), Map(startState -> Seq())) match {
              case Some(wfs.OpSeqResult(lastState, opSeq)) =>
                val nextStartState = wfs.updateStateBasedOnAllOperationActions(lastState, o)
                findOpSeqIterator(os, nextStartState, Seq(o) ++ opSeq ++ opSeqToReturn)
              case _ => None
            }

          }

          //          println("search started")
          val foundSeq = findOpSeqIterator(opSeqFromSpec) match {
            case Some(seq) =>
              println(s"The found sequence for specification $name contains ${seq.size} operations.")
              Sequence(seq.map(o => Hierarchy(o.id, List())): _*)
            case _ => EmptySOP
          }
          //          println("search ended")

          for {
            _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id, modelVersion = modelInfo.version, items = List(SOPSpec(name = s"${name}_Result", sop = List(foundSeq), attributes = SPAttributes().addTimeStamp))))
          } yield {}
        }
      }
    }

      sender ! "ok"

  }

  def getInitState(vars: Set[Thing]) = {
    val state = vars.foldLeft(Map(): Map[ID, SPValue]) { case (acc, v) =>
      v.attributes.findField(f => f._1 == "init") match {
        case Some(kv) => acc + (v.id -> kv._2)
        case _ => acc
      }
    }
    State(state)
  }

  def stateIsMarked(vars: Set[Thing]): State => Boolean = { thatState =>
    val goalState = vars.foldLeft(Map(): Map[ID, SPValue]) { case (acc, v) =>
      v.attributes.findAs[String]("goal") match {
        case i :: Nil => acc + (v.id -> Integer.parseInt(i))
        case _ => acc
      }
    }
    def checkState(stateToCheck: Seq[(ID, SPValue)] = thatState.state.toSeq): Boolean = stateToCheck match {
      case kv +: rest => goalState.get(kv._1) match {
        case Some(v) => if (v.equals(kv._2)) checkState(rest) else false
        case _ => false //"stateToCheck" contains variables that is not in "goalState". This should although not happen...
      }
      case _ => true //"stateToCheck" == "goalState"
    }
    checkState()
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
        val enabledOps = freshStates.map(s => s -> ops.filter(_.conditions(0).eval(s)))
        val newStates = enabledOps.foldLeft(new LocalResult(Set(), visitedStates): LocalResult) { case (acc, (s, os)) =>
          val newAcc = os.foldLeft(acc: LocalResult) { case (accInner, o) => {
            val targetStateForOperationO = updateStateBasedOnAllOperationActions(s, o)
            if (accInner.vStates.contains(targetStateForOperationO))
            //Target state has been visited before. Take no action.
              accInner.copy()
            else
            //Target state has not been visited before. Add to accumulator
              LocalResult(accInner.fStates + targetStateForOperationO, Map(targetStateForOperationO -> (o +: accInner.vStates(s))) ++ accInner.vStates)
          }
          }
          newAcc
        }
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

}

object CreateInstanceModelFromTypeModelService {
  def props(modelHandler: ActorRef) = Props(classOf[CreateInstanceModelFromTypeModelService], modelHandler)
}