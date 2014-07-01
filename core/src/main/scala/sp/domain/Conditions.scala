package sp.domain

/**
 * Created by Kristofer on 2014-06-10.
 */
trait Condition {
  def guard: State => Boolean
  def action: State => State
  val attributes: SPAttributes
}

case class PropositionGuard(g: Proposition)
case class Action[T](svid: ID, value: T)
case class PropositionCondition(g: PropositionGuard,
                                a: List[Action[_]],
                                attributes: SPAttributes = SPAttributes(Map())) extends Condition {
  //TODO: Maybe change these def in conditions, else impl
  def guard = s => false;
  def action = s => s
}



// propositional logic conditions

sealed trait Proposition {
//  def eval(s: State): Boolean = this match {
//    case AND(x,y) => x.eval(s) && y.eval(s)
//    case OR(x,y) => x.eval(s) || y.eval(s)
//    case NOT(x) => !x.eval(s)
//    case EQ(x,y) => x.value(s) == y.value(s)
//    case NEQ(x,y) => x.value(s) != y.value(s)
//  }
}

sealed trait Operator extends Proposition
sealed trait Atom extends Proposition

case class AND(_1: Proposition, _2: Proposition) extends Operator
case class OR(_1: Proposition, _2: Proposition) extends Operator
case class NOT(p: Proposition) extends Operator


case class EQ(x: StateEvaluator, y: StateEvaluator) extends Atom
case class NEQ(x: StateEvaluator, y: StateEvaluator) extends Atom

trait StateEvaluator

case class SVIDEval(svid: ID) extends StateEvaluator
case class ValueHolder(v: Any) extends StateEvaluator

//TODO: add StateEvaluator for a+b, a+1 etc when nedded 140630





