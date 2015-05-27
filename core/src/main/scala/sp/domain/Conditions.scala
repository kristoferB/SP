package sp.domain


/**
 * Created by Kristofer on 2014-06-10.
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
                                attributes: SPAttributes = SPAttributes()) extends Condition {
}



sealed trait Proposition

object Proposition {
  def parseStr(str: String, idables: List[IDAble] = List()): Option[Proposition] = {
    val propParser = sp.domain.logic.PropositionParser(idables)
    propParser.parseStr(str).right.toOption
  }

  implicit def strToProp(str: String)(implicit idables: List[IDAble] = List()): Proposition = parseStr(str, idables).get
}

case class AND(props: List[Proposition]) extends Proposition
case class OR(props: List[Proposition]) extends Proposition
case class NOT(p: Proposition) extends Proposition
case class EQ(left: StateEvaluator, right: StateEvaluator) extends Proposition
case class NEQ(left: StateEvaluator, right: StateEvaluator) extends Proposition
case class GREQ(left: StateEvaluator, right: StateEvaluator) extends Proposition
case class LEEQ(left: StateEvaluator, right: StateEvaluator) extends Proposition
case class GR(left: StateEvaluator, right: StateEvaluator) extends Proposition
case class LE(left: StateEvaluator, right: StateEvaluator) extends Proposition

// dummy Propositions used in algorithms
case object AlwaysTrue extends Proposition

case object AlwaysFalse extends Proposition

trait StateEvaluator

object StateEvaluator {
  implicit def idToSE(id: ID) = SVIDEval(id)
  implicit def strToSE(value: String) = ValueHolder(org.json4s.JString(value))
  implicit def intToSE(value: Int) = ValueHolder(org.json4s.JInt(value))
}

case class SVIDEval(id: ID) extends StateEvaluator {
  //override def toString = id.toString
}

case class SVNameEval(v: String) extends StateEvaluator {
  //override def toString = v
}

case class ValueHolder(v: SPValue) extends StateEvaluator with StateUpdater {
  //override def toString = v.toString
}

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
