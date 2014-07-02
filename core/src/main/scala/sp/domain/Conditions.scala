package sp.domain

/**
 * Created by Kristofer on 2014-06-10.
 */
trait Condition {
  def guard: State => Boolean
  def action: State => State
  val attributes: SPAttributes
}

case class Action(svid: ID, value: SPAttributeValue)
case class PropositionCondition(g: Proposition,
                                a: List[Action],
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

//sealed trait Operator extends Proposition
//sealed trait Atom extends Proposition

case class AND(left: Proposition, right: Proposition) extends Proposition
case class OR(left: Proposition, right: Proposition) extends Proposition
case class NOT(p: Proposition) extends Proposition


case class EQ(left: StateEvaluator, right: StateEvaluator) extends Proposition
case class NEQ(left: StateEvaluator, right: StateEvaluator) extends Proposition

trait StateEvaluator

case class SVIDEval(v: ID) extends StateEvaluator
case class ValueHolder(v: SPAttributeValue) extends StateEvaluator

//TODO: add StateEvaluator for a+b, a+1 etc when nedded 140630





