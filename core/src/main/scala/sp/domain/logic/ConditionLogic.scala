package sp.domain.logic

import sp.domain._


case object ConditionLogic {

  /**
   * Evaluate Propositions by calling eval(proposition, state)
   */
  implicit class propLogic(p: Proposition) {
    def eval(s: State): Boolean = {
      def req(p: Proposition): Boolean = {
        p match {
          case AND(props) => !props.exists(x => !req(x))
          case OR(props) => props exists req
          case NOT(prop) => !req(prop)
          case EQ(l, r) => getValue(l) == getValue(r)
          case NEQ(l, r) => getValue(l) != getValue(r)
        }
      }
      def getValue(se: StateEvaluator): SPAttributeValue = se match {
        case SVIDEval(id) => s(id)
        case ValueHolder(v) => v
        // does not handle SVNameEval
      }
      req(p)
    }
  }

  implicit class condLogic(c: Condition) {
    def eval(s: State) = {

    }
  }
}


import scala.util.parsing.combinator._

/**
 * Parses Propositions from a String and return an
 * Either, were Left(NoSucess) and Right(Proposition)
 */
case object PropositionParser extends JavaTokenParsers {

  def parseStr(str: String) = parseAll(or, str) match {
    case Success(result, _) => Right(result)
    case failure: NoSuccess => Left(failure) //failure.toString = "["+failure.next.pos+"] error: "+failure.msg+"\n\n"+failure.next.pos.longString
  }

  final lazy val REG_EX_OR = s"or|OR|Or|\\|\\||\\|".r
  final lazy val REG_EX_AND = s"and|AND|And|&&|&".r
  final lazy val REG_EX_NOT = s"not|n|!".r
  final lazy val REG_EX_TRUE = s"true|TRUE|T".r
  final lazy val REG_EX_FALSE = s"false|FALSE|F".r
  final lazy val REX_EX_VARIABLE = s"\\w+".r
  //A word build up from characters [A-Za-z0-9_] that is at least of length one.
  final lazy val REG_EX_OPERATOREQ = s"==|:=|=".r
  final lazy val REG_EX_OPERATORNEQ = s"!=".r
  final lazy val REG_EX_OPERATOR = s"==|>=|<=|!=|:=|=|>|<".r

  final lazy val REG_EX_VALUE = s"\\w+".r


  lazy val or: Parser[Proposition] = and ~ rep(REG_EX_OR ~ and) ^^ { case f1 ~ temp => if (temp.isEmpty) f1 else OR(f1 +: temp.map { case ~(_, f2) => f2})}
  lazy val and: Parser[Proposition] = not ~ rep(REG_EX_AND ~ not) ^^ { case f1 ~ temp => if (temp.isEmpty) f1 else AND(f1 +: temp.map { case ~(_, f2) => f2})}
  lazy val not: Parser[Proposition] = opt(REG_EX_NOT) ~ factor ^^ { case Some(_) ~ f => NOT(f); case None ~ f => f}
  lazy val factor: Parser[Proposition] = expressionEQ | expressionNEQ | "(" ~> or <~ ")" ^^ { case exp => exp}
  lazy val expressionEQ: Parser[Proposition] = REX_EX_VARIABLE ~ REG_EX_OPERATOREQ ~ REG_EX_VALUE ^^ { case ~(~(var1, op), v) => EQ(SVNameEval(var1), ValueHolder(v))}
  lazy val expressionNEQ: Parser[Proposition] = REX_EX_VARIABLE ~ REG_EX_OPERATORNEQ ~ REG_EX_VALUE ^^ { case ~(~(var1, op), v) => NEQ(SVNameEval(var1), ValueHolder(v))}
}

