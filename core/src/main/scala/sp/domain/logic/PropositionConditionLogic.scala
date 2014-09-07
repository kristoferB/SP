package sp.domain.logic

import sp.domain._


case object PropositionConditionLogic {


  /**
   * Evaluate Propositions by calling eval(proposition, state)
   */
  implicit class propLogic(p: Proposition) {
    def eval(s: State): Boolean = {
      def req(p: Proposition): Boolean = {
        p match {
          case AND(props) => !props.exists(!req(_))
          case OR(props) => props exists req
          case NOT(prop) => !req(prop)
          case EQ(l, r) => getValue(l) == getValue(r)
          case NEQ(l, r) => getValue(l) != getValue(r)
          case AlwaysTrue => true
          case AlwaysFalse => false
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

  /**
   * Condition logic
   * @param cond: Condition, but must be of type PropositionCondition
   */
  implicit class condLogic(cond: Condition) {
    val c = cond.asInstanceOf[PropositionCondition]
    def eval(s: State) = c.guard.eval(s)
    def next(s: State) = s.next(c.action map (a => a.stateVariableID -> a.nextValue(s)) toMap)
    def inDomain(s: State, stateVars: Map[ID, StateVariable]) = {
      !(c.action map(_.inDomain(s, stateVars)) exists (!_))
    }
  }

  implicit class nextLogic(a: Action) {
    def nextValue(s: State) = a.value match {
      case ValueHolder(v) => v
      case INCR(n) => SPAttributeValue(s(a.stateVariableID).asInt map (_ + n))
      case DECR(n) => SPAttributeValue(s(a.stateVariableID).asInt map (_ - n))
      case ASSIGN(id) => s(id)
    }

    def inDomain(s: State, stateVars: Map[ID, StateVariable]) = {
      val next = nextValue(s)
      val sv = stateVars(a.stateVariableID)

      val checkDomain = sv.attributes.getAsList("domain") map (_.contains(next))
      val checkBoolean = sv.attributes.getAsBool("boolean") map (b =>
        next.isInstanceOf[BoolPrimitive] && b)
      val checkRange = for {
        range <- sv.attributes.getAsMap("range")
        start <- range.get("start") flatMap (_.asInt)
        end <- range.get("end") flatMap (_.asInt)
        step <- range.get("step") flatMap (_.asInt)
        nextInt <- next.asInt
      } yield {
        val r = start until end by step
        r.contains(nextInt)
      }

      checkDomain getOrElse( checkRange getOrElse( checkBoolean getOrElse false))

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
  final lazy val REG_EX_UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r


  lazy val or: Parser[Proposition] = and ~ rep(REG_EX_OR ~ and) ^^ { case f1 ~ temp => if (temp.isEmpty) f1 else OR(f1 +: temp.map { case ~(_, f2) => f2})}
  lazy val and: Parser[Proposition] = not ~ rep(REG_EX_AND ~ not) ^^ { case f1 ~ temp => if (temp.isEmpty) f1 else AND(f1 +: temp.map { case ~(_, f2) => f2})}
  lazy val not: Parser[Proposition] = opt(REG_EX_NOT) ~ factor ^^ { case Some(_) ~ f => NOT(f); case None ~ f => f}
  lazy val factor: Parser[Proposition] = expressionEQ | expressionNEQ | "(" ~> or <~ ")" ^^ { case exp => exp}
  lazy val expressionEQ: Parser[Proposition] = stateEv ~ REG_EX_OPERATOREQ ~ stateEv ^^ { case ~(~(var1, op), v) => EQ(var1, v)}
  lazy val expressionNEQ: Parser[Proposition] = stateEv ~ REG_EX_OPERATORNEQ ~ stateEv ^^ { case ~(~(var1, op), v) => NEQ(var1, v)}
  lazy val stateEv: Parser[StateEvaluator] = uuid | value
  lazy val uuid: Parser[StateEvaluator] = REG_EX_UUID ^^ { uuid => SVIDEval(ID.makeID(uuid).get)}
  lazy val value: Parser[StateEvaluator] = REG_EX_VALUE ^^ { v => ValueHolder(StringPrimitive(v))}
}

//object TestParser extends App  {
//  println("Welcome to the parser: Type an expression or enter for exit")
//  waitForString
//
//  private def waitForString    {
//      def waitEOF(): Unit = Console.readLine() match {
//        case "" => ""
//        case "exit" => ""
//        case str: String => {
//          println(PropositionParser.parseStr(str))
//          println(PropositionParser.parseAll(PropositionParser.stateEv, str))
//          waitEOF()
//        }
//      }
//      waitEOF()
//    }
//}