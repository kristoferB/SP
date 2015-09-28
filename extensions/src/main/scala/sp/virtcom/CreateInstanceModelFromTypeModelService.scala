package sp.virtcom

import akka.actor.{Props, Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
import sp.jsonImporter.ServiceSupportTrait
import sp.system.SPService
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

class CreateInstanceModelFromTypeModelService(modelHandler: ActorRef) extends Actor {
  def receive = {
    case r @ Request(service, attr, ids, reqID) =>
      println(s"service: $service got reqID: $reqID")
      context.actorOf(Props(classOf[CreateInstanceModelFromTypeModelRunner], modelHandler)).tell(r, sender())
  }
}

object CreateInstanceModelFromTypeModelService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description"-> "Create one instance form type models"
    ),
    "selectedItems" -> SPAttributes(
      "selectedItems"-> KeyDefinition("List[ID]", List(), Some(SPValue(List())))
    )
  )

  // important to incl ServiceLauncher if you want unique actors per request
  def props(modelHandler: ActorRef) = Props(classOf[CreateInstanceModelFromTypeModelService], modelHandler)


  // Alla far aven "core" -> ServiceHandlerAttributes

  //  case class ServiceHandlerAttributes(model: Option[ID],
  //                                      responseToModel: Boolean,
  //                                      onlyResponse: Boolean,
  //                                      includeIDAbles: List[ID])

}


class CreateInstanceModelFromTypeModelRunner(modelHandler: ActorRef) extends Actor with ServiceSupportTrait {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(service, attr, _, _) => {

      println(s"service: $service")

      lazy val activeModel = attr.getAs[SPAttributes]("activeModel")
      lazy val selectedItems = attr.getAs[List[SPAttributes]]("selectedItems").map( _.flatMap(_.getAs[ID]("id"))).getOrElse(List())

      lazy val id = activeModel.flatMap(_.getAs[ID]("id")).getOrElse(ID.newID)

      //Search for operation sequence.
      for {
        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))

        //Get operations and variables
        SPIDs(opsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetOperations(model = modelInfo.id))
        ops = opsToBe.map(_.asInstanceOf[Operation])
        SPIDs(varsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetThings(model = modelInfo.id))
        vars = varsToBe.map(_.asInstanceOf[Thing])

        //Get specification for the operation sequence to return
        SPIDs(spSpecToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetSpecs(model = modelInfo.id,
          filter = {obj => selectedItems.contains(obj.id) && obj.isInstanceOf[SOPSpec]}))
        specs = spSpecToBe.map(_.asInstanceOf[SOPSpec])

        specList = {
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

      } yield {

        val initState = getIdleState(vars.toSet)

        val markedState = stateIsMarked(vars.toSet)

        import sp.domain.logic.PropositionConditionLogic._

        if (specList.isEmpty) println("No specification(s) given.")

        specList.foreach { case (name, opSeqFromSpec) =>

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
          val foundSeq = findOpSeqIterator(opSeqFromSpec) match {
            case Some(seq) =>
              println(s"The found sequence for specification \'$name\' contains ${seq.size} operations.")
              Sequence(seq.map(o => Hierarchy(o.id, List())): _*)
            case _ =>
              println(s"No sequence for specification \'$name\' could be found.")
              EmptySOP
          }
                    println("search ended")

          for {
            _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id,
              items = List(SOPSpec(name = s"${name}_Result", sop = List(foundSeq), attributes = SPAttributes().addTimeStamp)),
            info = SPAttributes("info" -> s"Added SOP ${name}_Result")))
          } yield {}
        }
      }
    }

      sender ! "ok"

  }

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

}