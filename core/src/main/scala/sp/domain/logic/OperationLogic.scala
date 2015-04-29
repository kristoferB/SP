package sp.domain.logic

import sp.domain._

case object OperationLogic {

  case class EvaluateProp(
    stateVars: Map[ID, SPAttributeValue => Boolean],
    groups: Set[SPAttributeValue],
    defs: OperationStateDefinition = TwoStateDefinition
  )

  implicit class opLogic(o: Operation) {
    import PropositionConditionLogic._
    def eval(s: State)(implicit props: EvaluateProp) = {
      
      val opState = s(o.id)
      if (props.defs.completed(opState)) false
      else {
        val filtered = props.defs.filterConds(opState, o.conditions)
        val inDomain = filtered exists (_.inDomain(s, props.stateVars))
        inDomain && !(filtered exists (!_.eval(s))) || filtered.isEmpty
      }
    }

    def next(s: State)(implicit props: EvaluateProp) = {
      props.defs.nextState(o, s)
    }



    def inDomain = OperationState.inDomain
  }


  object OperationState {
    val init: SPAttributeValue = "i"
    val executing: SPAttributeValue = "e"
    val finished: SPAttributeValue = "f"
    val domain = Set(init, executing, finished)
    def inDomain = domain.contains(_)
  }

  import OperationState._


  import PropositionConditionLogic._
  trait OperationStateDefinition {
    def nextState(operation: Operation, state: State)(implicit props: EvaluateProp): State
    def domain: List[SPAttributeValue]

    def completed(state: SPAttributeValue) = {
      state == finished
    }
    def kinds(state: SPAttributeValue): Set[SPAttributeValue] = {
      if (state == init) Set("pre", "precondition")
      else if (state == executing) Set("post", "postcondition")
      else if (state == finished) Set("reset", "resetcondition")
      else throw new IllegalArgumentException(s"Can not understand operation state: $state")
    }
    def filterConds(opState: SPAttributeValue, conds: List[Condition])(implicit props: EvaluateProp) = {
      val kinds = props.defs.kinds(opState)
      val groups = if (props.groups.isEmpty) props.groups else props.groups + ""
      val groupCond = filter("group", conds, groups)
      filter("kind", groupCond, kinds)
    }

    def filter(filter: String, conds: List[Condition], set: Set[SPAttributeValue]) = {
      conds filter(c => {
        val res = c.attributes.get(filter).getOrElse(SPAttributeValue(""))
        (set contains res) || set.isEmpty
      })
    }

    protected[OperationStateDefinition] def next(o: Operation, state: State)(implicit props: EvaluateProp) = {
      val opState = state(o.id)
      val filtered = filterConds(opState, o.conditions)

      val newState = filtered.foldLeft(state){(s, cond) => cond.next(s)}
      newState.next(o.id -> nextOpState(opState))
    }

    protected[OperationStateDefinition] def nextOpState(state: SPAttributeValue) = {
      if (state == init) executing
      else if (state == executing) finished
      else if (state == finished) init
      else throw new IllegalArgumentException(s"Can not understand operation state: $state")
    }
  }



  case object ThreeStateDefinition extends OperationStateDefinition{
    
    def domain = List(init, executing, finished)

    def nextState(o: Operation, state: State)(implicit props: EvaluateProp): State = {
      next(o,state)
    }

  }

  case object TwoStateDefinition extends OperationStateDefinition{
    import OperationState._
    def domain = List(init, finished)

    def nextState(o: Operation, state: State)(implicit props: EvaluateProp): State = {
      next(o,next(o,state))

    }


  }

}
