package sp.domain


/**
 * Created by Kristofer on 2014-06-10.
 *
 * TODO: Move logic to Proposition Logic! 150531 KB
 * TODO: Also simplify the condition structure as soon as possible,
 * TODO: e.g. remove stateEvaluater and StateUpdater
 */
trait Condition {
  val attributes: SPAttributes
}

case class Action(id: ID, value: StateUpdater)
object Action {
  def parseStr(str: String, idables: List[IDAble] = List()): Option[Action] = {
    val propParser = sp.domain.logic.ActionParser(idables)
    propParser.parseStr(str).right.toOption
  }

  implicit def strToProp(str: String)(implicit idables: List[IDAble] = List()): Action = parseStr(str, idables).get
}

case class PropositionCondition(guard: Proposition,
                                action: List[Action],
                                attributes: SPAttributes = SPAttributes()) extends Condition



sealed trait Proposition

object Proposition {
  def parseStr(str: String, idables: List[IDAble] = List()): Option[Proposition] = {
    val propParser = sp.domain.logic.PropositionParser(idables)
    propParser.parseStr(str).right.toOption
  }

  implicit def strToProp(str: String)(implicit idables: List[IDAble] = List()): Proposition = parseStr(str, idables).get
}

trait PropositionEvaluator extends Proposition {
  val left: StateEvaluator
  val right: StateEvaluator
}

case class AND(props: List[Proposition]) extends Proposition
case class OR(props: List[Proposition]) extends Proposition
case class NOT(p: Proposition) extends Proposition
case class EQ(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator
case class NEQ(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator
case class GREQ(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator
case class LEEQ(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator
case class GR(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator
case class LE(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator

// dummy Propositions used in algorithms
case object AlwaysTrue extends Proposition

case object AlwaysFalse extends Proposition

trait StateEvaluator

object StateEvaluator {
  implicit def idToSE(id: ID) = SVIDEval(id)
  implicit def strToSE(value: String) = ValueHolder(org.json4s.JString(value))
  implicit def intToSE(value: Int) = ValueHolder(org.json4s.JInt(value))
}

case class SVIDEval(id: ID) extends StateEvaluator
case class ValueHolder(v: SPValue) extends StateEvaluator with StateUpdater

//TODO: add StateEvaluator for a+b, a+1 etc when nedded 140630

sealed trait StateUpdater
case class INCR(n: Int) extends StateUpdater
case class DECR(n: Int) extends StateUpdater
case class ASSIGN(id: ID) extends StateUpdater

private object StrMaker {
  def makeStr[T](xs: List[T], div: String) = {
    def req(list: List[T]): String = list match {
      case x :: Nil => x.toString
      case x :: xs => s"$x $div ${req(xs)}"
    }
    if (xs.isEmpty) s"$div EMPTY"
    else s"(${req(xs)})"
  }
}
