package sp.domain.logic

import sp.domain._

case object OperationLogic extends OperationLogics {

}

trait OperationLogics {
  import sp.domain.logic.AttributeLogic._
  import sp.domain.logic.StateLogic._

  case class EvaluateProp(
    stateVars: Map[ID, SPValue => Boolean],
    groups: Set[SPValue],
    defs: OperationStateDefinition = TwoStateDefinition
  )

  implicit class opLogic(o: Operation) {
    import PropositionConditionLogic._
    def eval(s: SPState)(implicit props: EvaluateProp) = {

      val opState = s(o.id)
      if (props.defs.completed(opState)) false
      else {
        val filtered = props.defs.filterConds(opState, o.conditions)
        val inDomain = filtered exists (_.inDomain(s, props.stateVars))
        inDomain && !(filtered exists (!_.eval(s))) || filtered.isEmpty
      }
    }

    def next(s: SPState)(implicit props: EvaluateProp) = {
      props.defs.nextState(o, s)
    }



    def inDomain = OperationState.inDomain
  }


  object OperationState {
    val init: SPValue = "i"
    val executing: SPValue = "e"
    val finished: SPValue = "f"
    val domain = Set(init, executing, finished)
    def inDomain = domain.contains(_)
  }

  import OperationState._


  import PropositionConditionLogic._
  trait OperationStateDefinition {
    def nextState(operation: Operation, state: SPState)(implicit props: EvaluateProp): SPState
    def domain: List[SPValue]

    def completed(state: SPValue) = {
      state == finished
    }
    def kinds(state: SPValue): Set[SPValue] = {
      if (state == init) Set("pre", "precondition")
      else if (state == executing) Set("post", "postcondition")
      else if (state == finished) Set("reset", "resetcondition")
      else throw new IllegalArgumentException(s"Can not understand operation state: $state")
    }
    def filterConds(opState: SPValue, conds: List[Condition])(implicit props: EvaluateProp) = {
      val kinds = props.defs.kinds(opState)
      val groups = if (props.groups.isEmpty) props.groups else props.groups + ""
      val groupCond = filter("group", conds, groups)
      filter("kind", groupCond, kinds)
    }

    def filter(filter: String, conds: List[Condition], set: Set[SPValue]) = {
      conds filter(c => {
        val res: SPValue = c.attributes.get(filter).getOrElse("")
        (set contains res) || set.isEmpty
      })
    }

    protected[OperationStateDefinition] def next(o: Operation, state: SPState)(implicit props: EvaluateProp) = {
      val opState = state(o.id)
      val filtered = filterConds(opState, o.conditions)

      val newState = filtered.foldLeft(state){(s, cond) => cond.next(s)}
      newState.next(o.id -> nextOpState(opState))
    }

    protected[OperationStateDefinition] def nextOpState(state: SPValue) = {
      if (state == init) executing
      else if (state == executing) finished
      else if (state == finished) init
      else throw new IllegalArgumentException(s"Can not understand operation state: $state")
    }
  }


  case object ThreeStateDefinitionWithReset extends OperationStateDefinition {
    def domain = List(init, executing)

    override def nextOpState(state: SPValue) = {
      if (state == init) executing
      else if (state == executing) init
      else throw new IllegalArgumentException(s"Can not understand operation state: $state")
    }

    def nextState(o: Operation, state: SPState)(implicit props: EvaluateProp): SPState = {
      next(o,state)
    }
  }

  case object ThreeStateDefinition extends OperationStateDefinition{
    
    def domain = List(init, executing, finished)

    def nextState(o: Operation, state: SPState)(implicit props: EvaluateProp): SPState = {
      next(o,state)
    }

  }

  case object TwoStateDefinition extends OperationStateDefinition{
    import OperationState._
    def domain = List(init, finished)

    def nextState(o: Operation, state: SPState)(implicit props: EvaluateProp): SPState = {
      next(o,next(o,state))

    }


  }

}
