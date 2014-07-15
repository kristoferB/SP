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

case class AND(props: List[Proposition]) extends Proposition
case class OR(props: List[Proposition]) extends Proposition
case class NOT(p: Proposition) extends Proposition

case class EQ(left: StateEvaluator, right: StateEvaluator) extends Proposition
case class NEQ(left: StateEvaluator, right: StateEvaluator) extends Proposition

trait StateEvaluator

case class SVIDEval(v: ID) extends StateEvaluator
case class SVNameEval(v: String) extends StateEvaluator
case class ValueHolder(v: SPAttributeValue) extends StateEvaluator

//TODO: add StateEvaluator for a+b, a+1 etc when nedded 140630



// test
import scala.util.parsing.combinator._
sealed trait Regix {
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
}

trait LogicalExpressionParser extends JavaTokenParsers with Regix {
  def parseStr(str: String) = parseAll(or, str) match {
    case Success(result, _) => Right(result)
    case failure: NoSuccess => Left(failure) //failure.toString = "["+failure.next.pos+"] error: "+failure.msg+"\n\n"+failure.next.pos.longString
  }
  lazy val or: Parser[Proposition] = and ~ rep(REG_EX_OR ~ and) ^^ { case f1 ~ temp => if(temp.isEmpty) f1 else OR(f1 +: temp.map { case ~(_, f2) => f2 }) }
  lazy val and: Parser[Proposition] = not ~ rep(REG_EX_AND ~ not) ^^ { case f1 ~ temp => if(temp.isEmpty) f1 else AND(f1 +: temp.map { case ~(_, f2) => f2 }) }
  lazy val not: Parser[Proposition] = opt(REG_EX_NOT) ~ factor ^^ { case Some(_) ~ f => NOT(f); case None ~ f => f }
  lazy val factor: Parser[Proposition] = expressionEQ | expressionNEQ | "(" ~> or <~ ")" ^^ { case exp => exp }
  lazy val expressionEQ: Parser[Proposition] = REX_EX_VARIABLE ~ REG_EX_OPERATOREQ ~ REG_EX_VALUE ^^ { case ~(~(var1, op), v) => EQ(SVNameEval(var1), ValueHolder(v)) }
  lazy val expressionNEQ: Parser[Proposition] = REX_EX_VARIABLE ~ REG_EX_OPERATORNEQ ~ REG_EX_VALUE ^^ { case ~(~(var1, op), v) => NEQ(SVNameEval(var1), ValueHolder(v)) }
}


object TestParser extends App with LogicalExpressionParser {
  println("Welcome to the parser: Type an expression or enter for exit")
  waitForString

  private def waitForString    {
      def waitEOF(): Unit = Console.readLine() match {
        case "" => ""
        case "exit" => ""
        case str: String => println(clean(parseStr(str))); waitEOF()
      }
      waitEOF()
    }


//  val tests = Seq(
//    "true and (false or T)",
//    "!(rt==5) And ty >=   			 true")
//  tests.foreach(str => println(clean(parseStr(str))))
//  val test2 = parseStr("true OR false OR true")
//  println (test2)
//  println(clean(test2))

  //Updated the parser => This method is no longer need? /Patrik 140715
  def clean(ex: Either[TestParser.NoSuccess, Proposition]) = {
    def req(left: Proposition): Proposition = {
      left match {
        case OR(xs) => if (xs.size == 1) req(xs.head) else OR(xs map req)
        case AND(xs) => if (xs.size == 1) req(xs.head) else AND(xs map req)
        case NOT(x) => NOT(req(x))
        case p: Proposition => p
      }
    }
    ex match {
        case Right(expr) => Right(req(expr))
        case _ => ex
      }
  }

}