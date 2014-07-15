import scala.util.parsing.combinator._

/**
 * Created by Patrik
 * Structure for parser of {@link Conditions} and/or {@link Propositions}
 */
sealed trait SPExpression {
  final lazy val REG_EX_OR = s"or|OR|Or|\\|\\||\\|".r
  final lazy val REG_EX_AND = s"and|AND|And|&&|&".r
  final lazy val REG_EX_NOT = s"not|n|!".r
  final lazy val REG_EX_TRUE = s"true|TRUE|T".r
  final lazy val REG_EX_FALSE = s"false|FALSE|F".r
  final lazy val REX_EX_VARIABLE = s"\\w+".r //A word build up from characters [A-Za-z0-9_] that is at least of length one.
  final lazy val REG_EX_OPERATOR = s"==|>=|<=|!=|:=|=|>|<".r
  final lazy val REG_EX_VALUE = s"\\w+".r
}

sealed trait SPFormulaExpression extends SPExpression {
  val formula: List[SPExpression]
  val formulaSep: String
  //override def toString = formula.map { f => f match { case fe: SPFormulaExpression if fe.formula.size > 1 => s"($f)"; case _ => s"$f" } }.mkString(formulaSep)
}

case class Expression(variable: String, operator: String, value: String) extends SPExpression //{ override def toString = s"$variable $operator $value" }
case class OR(formula: List[SPExpression]) extends SPFormulaExpression { lazy val formulaSep = " | " }
case class AND(formula: List[SPExpression]) extends SPFormulaExpression { lazy val formulaSep = " & " }
case class NOT(exp: SPExpression) extends SPExpression //{ override def toString = s"!$exp" }
object TRUE extends SPExpression //{ override def toString = "true" }
object FALSE extends SPExpression //{ override def toString = "false" }

trait LogicalExpressionParser extends JavaTokenParsers with SPExpression {
  def parseStr(str: String) = parseAll(or, str) match {
    case Success(result, _) => Right(result)
    case failure: NoSuccess => Left(failure) //failure.toString = "["+failure.next.pos+"] error: "+failure.msg+"\n\n"+failure.next.pos.longString
  }
  lazy val or: Parser[SPExpression] = and ~ rep(REG_EX_OR ~ and) ^^ { case f1 ~ temp => if(temp.isEmpty) f1 else OR(f1 +: temp.map { case ~(_, f2) => f2 }) }
  lazy val and: Parser[SPExpression] = not ~ rep(REG_EX_AND ~ not) ^^ { case f1 ~ temp => if(temp.isEmpty) f1 else AND(f1 +: temp.map { case ~(_, f2) => f2 }) }
  lazy val not: Parser[SPExpression] = opt(REG_EX_NOT) ~ factor ^^ { case Some(_) ~ f => NOT(f); case None ~ f => f }
  lazy val factor: Parser[SPExpression] = literal | expression | "(" ~> or <~ ")" ^^ { case exp => exp }
  lazy val literal: Parser[SPExpression] = REG_EX_TRUE ^^ (_ => TRUE) | REG_EX_FALSE ^^ (_ => FALSE)
  lazy val expression: Parser[SPExpression] = REX_EX_VARIABLE ~ REG_EX_OPERATOR ~ REG_EX_VALUE ^^ { case ~(~(var1, op), v) => Expression(var1, op, v) }
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


  def clean(ex: Either[TestParser.NoSuccess, SPExpression]) = {
    def req(left: SPExpression): SPExpression = {
      left match {
        case f: SPFormulaExpression if f.formula.size == 1 => req(f.formula.head)
        case OR(xs) => OR(xs map req)
        case AND(xs) => AND(xs map req)
        case NOT(x) => NOT(req(x))
        case sp: SPExpression => sp
      }
    }
    ex match {
        case Right(expr) => Right(req(expr))
        case _ => ex
      }
  }

}