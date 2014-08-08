package sp.domain.logic

import sp.domain._

case object OperationLogic {

  case class EvaluateProp(
    stateVars: Map[ID, StateVariable],
    groups: Set[SPAttributeValue],
    defs: OperationStateDefinition = TwoStateDefinition
  )

  implicit class opLogic(o: Operation) {
    import PropositionConditionLogic._
    def eval(s: State)(implicit props: EvaluateProp) = {
      val opState = s(o.id)
      if (props.defs.completed(opState)) false
      else {
        val filtered = filterConds(opState, o.conditions)
        val inDomain = filtered exists (_.inDomain(s, props.stateVars))
        inDomain && !(filtered exists (!_.eval(s))) || filtered.isEmpty
      }
    }

    def next(s: State)(implicit props: EvaluateProp) = {
      val opState = s(o.id)
      val filtered = filterConds(opState, o.conditions)

      val newState = filtered.foldLeft(s){(state, cond) => cond.next(state)}
      newState.next(o.id -> props.defs.nextState(opState))
    }

    private def filterConds(opState: SPAttributeValue, conds: List[Condition])(implicit props: EvaluateProp) = {
      val kinds = props.defs.kinds(opState)
      val groupCond = filter("group", conds, props.groups + "")
      filter("kind", groupCond, kinds)
    }

    private def filter(filter: String, conds: List[Condition], set: Set[SPAttributeValue]) = {
      conds filter(c => {
        val res = c.attributes.get(filter).getOrElse(SPAttributeValue(""))
        (set contains res) || set.isEmpty
      })
    }
  }




  trait OperationStateDefinition {
    def completed(state: SPAttributeValue): Boolean
    def kinds(state: SPAttributeValue): Set[SPAttributeValue]
    def nextState(state: SPAttributeValue): SPAttributeValue
    def domain: List[SPAttributeValue]
  }


  case object ThreeStateDefinition extends OperationStateDefinition{
    val init: SPAttributeValue = "i"
    val executing: SPAttributeValue = "e"
    val finished: SPAttributeValue = "f"
    def domain = List(init, executing, finished)

    def completed(state: SPAttributeValue) = {
      state == finished
    }

    def kinds(state: SPAttributeValue) = {
      if (state == init) Set("pre", "precondition")
      else if (state == executing) Set("post", "postcondition")
      else if (state == finished) Set("reset", "resetcondition")
      else throw new IllegalArgumentException(s"Can not understand operation state: $state")
    }
    def nextState(state: SPAttributeValue) = {
      if (state == init) executing
      else if (state == executing) finished
      else if (state == finished) init
      else throw new IllegalArgumentException(s"Can not understand operation state: $state")
    }
  }

  case object TwoStateDefinition extends OperationStateDefinition{
    val init: SPAttributeValue = "i"
    val finished: SPAttributeValue = "f"
    def domain = List(init, finished)

    def completed(state: SPAttributeValue) = {
      state == finished
    }

    def kinds(state: SPAttributeValue) = {
      if (state == init) Set("pre", "precondition", "post", "postcondition", "")
      else if (state == finished) Set("reset", "resetcondition")
      else throw new IllegalArgumentException(s"Can not understand operation state: $state")
    }
    def nextState(state: SPAttributeValue) = {
      if (state == init) finished
      else if (state == finished) init
      else throw new IllegalArgumentException(s"Can not understand operation state: $state")
    }
  }

}
