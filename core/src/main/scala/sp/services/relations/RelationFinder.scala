package sp.services.relations

import akka.actor._
import sp.domain._

import scala.annotation.tailrec

/**
 * This message starts the identification. Returns the
 * relations identified.
 * TODO: I need to update this later for better performance. Mainly using
 * ints and arrays instead of all the objects.
 *
 * @param ops The ops should have all conditions
 *            that should be used. So add Specs
 *            before
 */
case class FindRelations(ops: List[Operation], stateVars: Map[ID, StateVariable], init: State)


class RelationFinder extends Actor with RelationFinderAlgotithms {
  def receive = {
    case FindRelations(ops, svs, init) => {

    }
  }
}


trait RelationFinderAlgotithms {


  case class SeqResult(seq: List[Operation], goalState: State, stateMap: Map[State, List[Operation]])
  def runASeq(ops: List[Operation],
              stateVars: Map[ID, StateVariable],
              init: State,
              goal: State => Boolean) = {

    import sp.domain.logic.OperationLogic._
    val opSV  = ops map(o => o.id -> StateVariable.operationVariable(o)) toMap
    // TODO: allow to start from other op states in the future
    val startState = init.next(ops map(o => o.id -> StringPrimitive("i")) toMap)
    implicit val props = EvaluateProp(stateVars ++ opSV, Set(""))

    @tailrec
    def req(ops: List[Operation], s: State, seq: List[Operation], stateMap: Map[State, List[Operation]]): SeqResult = {
      val enabled = ops.filter(o => o.eval(s))
      if (enabled.isEmpty || goal(s)){
        SeqResult(seq.reverse, s, stateMap)
      }
      else {
        val o = enabled(scala.util.Random.nextInt(enabled.size))
        val newState = o next s
        val remainingOps = ops.filterNot(_ == o)
        req(remainingOps, newState, o :: seq, stateMap + (s->enabled))
      }
    }

    req(ops, startState, List(), Map())
  }



}
