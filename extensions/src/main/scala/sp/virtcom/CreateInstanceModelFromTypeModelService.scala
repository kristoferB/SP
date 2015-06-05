package sp.virtcom

import akka.actor.{Props, Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
import sp.jsonImporter.ServiceSupportTrait
import sp.system.messages._
import scala.concurrent.duration._
import sp.domain.Logic._

/**
 * Created by patrik on 2015-06-03.
 */
class CreateInstanceModelFromTypeModelService(modelHandler: ActorRef) extends Actor with ServiceSupportTrait {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(service, attr) => {

      println(s"service: $service")

      val id = attr.getAs[ID]("activeModelID").getOrElse(ID.newID)

      //Search for operation sequence.
      val foundSeqAsSOP = for {
        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))
        SPIDs(opsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetOperations(model = modelInfo.model))
        ops = opsToBe.map(_.asInstanceOf[Operation])
        SPIDs(varsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetThings(model = modelInfo.model))
        vars = varsToBe.map(_.asInstanceOf[Thing])
      } yield {
          val initState = getInitState(vars.toSet)
          val markedState = stateIsMarked(vars.toSet)
          val opsMap = ops.map(o => o.name -> o).toMap

          import sp.domain.logic.PropositionConditionLogic._

          val wfs = WrapperForSearch(ops.toSet)
          val result = for {
            r1 <- wfs.findOpSeq(opsMap("gripRoof_ABB").conditions(0).eval, Set(initState), Map(initState -> Seq()))
            r2 <- wfs.findOpSeq(opsMap("fixateRoof_ABB").conditions(0).eval, (r1,opsMap("gripRoof_ABB")))
            r3 <- wfs.findOpSeq(markedState, (r2,opsMap("fixateRoof_ABB")))
          } yield {
              implicit def opToSeqOp(o: Operation): Seq[Operation] = Seq(o)
              val opSeq = r3.opSeq ++ opsMap("fixateRoof_ABB") ++ r2.opSeq ++ opsMap("gripRoof_ABB") ++ r1.opSeq
              opSeq.reverse
            }

          println("start search")
          result match {
            case Some(r) => println(r.map(_.name).mkString("\n"))
            case _ =>
          }
          println("end search")

          result match {
            case Some(seq) => Sequence(seq.map(o => Hierarchy(o.id, List())): _*)
            case _ => EmptySOP
          }
        }

      //Add sop result to model
      for {
        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))
        sop <- foundSeqAsSOP
        _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id, modelVersion = modelInfo.version, items = List(SOPSpec(name = "oneSequence", sop = List(sop), attributes = SPAttributes().addTimeStamp))))
      } yield {
        println(s"A new sop was generated! $sop")
      }

      sender ! "ok"

    }
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
    def checkState(stateToCheck : Seq[(ID,SPValue)] = thatState.state.toSeq) : Boolean = stateToCheck match {
      case kv+:rest => goalState.get(kv._1) match {
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

    def updateStateBasedInAllOperationActions(startState : State, o:Operation) = o.conditions.foldLeft(startState: State) { case (acc, c) => c.next(acc) }

    def findOpSeq(terminationCondition: State => Boolean, opSeqStartOpPair: (OpSeqResult,Operation)): Option[OpSeqResult] = {
      val updatedState = updateStateBasedInAllOperationActions(opSeqStartOpPair._1.finalState,opSeqStartOpPair._2)
      findOpSeq(terminationCondition, Set(updatedState), Map(updatedState -> Seq()))
    }
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
            val targetStateForOperationO = updateStateBasedInAllOperationActions(s,o)
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