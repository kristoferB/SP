package sp.domain


case class Condition(guard: Proposition,
                     action: List[Action] = List(),
                     attributes: SPAttributes = SPAttributes())


case class Action(id: ID, value: StateUpdater)

object Action {
  def parseStr(str: String, idables: List[IDAble] = List()): Option[Action] = {
    val propParser = sp.domain.logic.ActionParser(idables)
    propParser.parseStr(str).right.toOption
  }

  implicit def strToProp(str: String)(implicit idables: List[IDAble] = List()): Action = parseStr(str, idables).get

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
case object AlwaysTrue extends Proposition
case object AlwaysFalse extends Proposition


sealed trait PropositionEvaluator extends Proposition {
  val left: StateEvaluator
  val right: StateEvaluator
}
case class EQ(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator
case class NEQ(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator
case class GREQ(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator
case class LEEQ(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator
case class GR(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator
case class LE(left: StateEvaluator, right: StateEvaluator) extends PropositionEvaluator



sealed trait StateEvaluator
sealed trait StateUpdater

object StateEvaluator {
  implicit def idToSE(id: ID): SVIDEval = SVIDEval(id)
  implicit def strToSE(value: String): ValueHolder = ValueHolder(SPValue(value))
  implicit def intToSE(value: Int): ValueHolder = ValueHolder(SPValue(value))
}

case class SVIDEval(id: ID) extends StateEvaluator
case class ValueHolder(v: SPValue) extends StateEvaluator with StateUpdater

case class INCR(n: Int) extends StateUpdater
case class DECR(n: Int) extends StateUpdater
case class ASSIGN(id: ID) extends StateUpdater

private object StrMaker {
  def makeStr[T](xs: List[T], div: String) = {
    def req(list: List[T]): String = list match {
      case Nil => ""
      case x :: Nil => x.toString
      case x :: xs => s"$x $div ${req(xs)}"
    }
    if (xs.isEmpty) s"$div EMPTY"
    else s"(${req(xs)})"
  }
}
