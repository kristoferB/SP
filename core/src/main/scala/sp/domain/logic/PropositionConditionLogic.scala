package sp.domain.logic

import sp.domain._

case object PropositionConditionLogic {

  /**
   * Evaluate Propositions by calling eval(proposition, state)
   */
  implicit class propLogic(iProp: Proposition) {
    def eval(state: State): Boolean = {
      def req(prop: Proposition): Boolean = prop match {
        case AND(ps) => !ps.exists(!req(_))
        case OR(ps) => ps.exists(req)
        case NOT(p) => !req(p)
        case EQ(l, r) => getValue(l) == getValue(r)
        case NEQ(l, r) => getValue(l) != getValue(r)
        case AlwaysTrue => true
        case AlwaysFalse => false
      }

      def getValue(se: StateEvaluator): SPAttributeValue = se match {
        case SVIDEval(id) => state(id)
        case ValueHolder(v) => v
        // does not handle SVNameEval
      }
      req(iProp)
    }

    def eval_int(state: State): Option[Boolean] = {
      def req(prop: Proposition): Option[Boolean] = prop match {
        case AND(ps) => listBooleanEval(ps, list => !list.flatten.contains(false))
        case OR(ps) => listBooleanEval(ps, list => list.flatten.contains(true))
        case NOT(p) => for {b <- req(p)} yield !b
        case EQ(l, r) => numberEval(l, r, _ == _)
        case NEQ(l, r) => numberEval(l, r, _ != _)
        case GREQ(l, r) => numberEval(l, r, _ >= _)
        case LEEQ(l, r) => numberEval(l, r, _ <= _)
        case GR(l, r) => numberEval(l, r, _ > _)
        case LE(l, r) => numberEval(l, r, _ < _)
        case AlwaysTrue => Some(true)
        case AlwaysFalse => Some(false)
      }

      def listBooleanEval(ps: List[Proposition], e: List[Option[Boolean]] => Boolean) = {
        val psOptionValues = ps.map(req)
        if (psOptionValues.contains(None)) None else Some(e(psOptionValues))
      }
      def numberEval(l: StateEvaluator, r: StateEvaluator, e: (Int, Int) => Boolean) = (for {
        left <- getValue(l)
        right <- getValue(r)
      } yield e(left, right) ) match {
        case ok @ Some(_) => ok
        case _ => {println(s"Problem to do int evaluation for $l and $r with function $e") ; None}
      }

      def getValue(se: StateEvaluator): Option[Int] = se match {
        case SVIDEval(id) => state(id).asInt
        case ValueHolder(v) => v.asInt
        case _ => None
        // does not handle SVNameEval
      }

      req(iProp)
    }
  }

  /**
   * Condition logic
   * @param cond: Condition, but must be of type PropositionCondition
   */
  implicit class condLogic(cond: Condition) {
    val c = cond.asInstanceOf[PropositionCondition]
    def eval(s: State) = c.guard.eval(s)
    def next(s: State) = s.next(c.action map (a => a.id -> a.nextValue(s)) toMap)
    def inDomain(s: State, stateVars: Map[ID, SPAttributeValue => Boolean]) = {
      !(c.action map (_.inDomain(s, stateVars)) exists (!_))
    }
  }

  implicit class nextLogic(a: Action) {
    def nextValue(s: State) = a.value match {
      case ValueHolder(v) => v
      case INCR(n) => SPAttributeValue(s(a.id).asInt map (_ + n))
      case DECR(n) => SPAttributeValue(s(a.id).asInt map (_ - n))
      case ASSIGN(id) => s(id)
    }

    def inDomain(s: State, stateVars: Map[ID, SPAttributeValue => Boolean]): Boolean = {
      val next = nextValue(s)
      val sv = stateVars(a.id)
      stateVars(a.id)(next)

      //      val checkDomain = sv.attributes.getAsList("domain") map (_.contains(next))
      //      val checkBoolean = sv.attributes.getAsBool("boolean") map (b =>
      //        next.isInstanceOf[BoolPrimitive] && b)
      //      val checkRange = for {
      //        range <- sv.attributes.getAsMap("range")
      //        start <- range.get("start") flatMap (_.asInt)
      //        end <- range.get("end") flatMap (_.asInt)
      //        step <- range.get("step") flatMap (_.asInt)
      //        nextInt <- next.asInt
      //      } yield {
      //        val r = start until end by step
      //        r.contains(nextInt)
      //      }
      //
      //      checkDomain getOrElse( checkRange getOrElse( checkBoolean getOrElse false))

    }
  }

}

import scala.util.parsing.combinator._

/**
 * Parses Propositions from a String and return an
 * Either, were Left(NoSucess) and Right(Proposition)
 */
case class PropositionParser(idablesToParseFromString: List[IDAble] = List()) extends BaseParser {

  def parseStr(str: String) = parseAll(or, str) match {
    case Success(result, _) => Right(result)
    case failure: NoSuccess => Left(failure) //failure.toString = "["+failure.next.pos+"] error: "+failure.msg+"\n\n"+failure.next.pos.longString
  }

  final lazy val REG_EX_OR = s"or|OR|Or|\\|\\||\\|".r
  final lazy val REG_EX_AND = s"and|AND|And|&&|&".r
  final lazy val REG_EX_NOT = s"not|n|!".r

  final lazy val REG_EX_OPERATOREQ = s"==".r
  final lazy val REG_EX_OPERATORNEQ = s"!=".r
  final lazy val REG_EX_OPERATORGREQ = s">=".r
  final lazy val REG_EX_OPERATORLEEQ = s"<=".r
  final lazy val REG_EX_OPERATORGR = s">".r
  final lazy val REG_EX_OPERATORLE = s"<".r

  lazy val or: Parser[Proposition] = and ~ rep(REG_EX_OR ~ and) ^^ { case f1 ~ temp => if (temp.isEmpty) f1 else OR(f1 +: temp.map { case ~(_, f2) => f2 }) }
  lazy val and: Parser[Proposition] = not ~ rep(REG_EX_AND ~ not) ^^ { case f1 ~ temp => if (temp.isEmpty) f1 else AND(f1 +: temp.map { case ~(_, f2) => f2 }) }
  lazy val not: Parser[Proposition] = opt(REG_EX_NOT) ~ factor ^^ { case Some(_) ~ f => NOT(f); case None ~ f => f }
  lazy val factor: Parser[Proposition] = expressionEQ | expressionNEQ | expressionGREQ | expressionLEEQ | expressionGR | expressionLE | "(" ~> or <~ ")" ^^ { case exp => exp }
  lazy val expressionEQ: Parser[Proposition] = leftEv ~ REG_EX_OPERATOREQ ~ rightEv ^^ { case ~(~(var1, op), v) => EQ(var1, v) }
  lazy val expressionNEQ: Parser[Proposition] = leftEv ~ REG_EX_OPERATORNEQ ~ rightEv ^^ { case ~(~(var1, op), v) => NEQ(var1, v) }
  lazy val expressionGREQ: Parser[Proposition] = leftEv ~ REG_EX_OPERATORGREQ ~ rightEv ^^ { case ~(~(var1, op), v) => GREQ(var1, v) }
  lazy val expressionLEEQ: Parser[Proposition] = leftEv ~ REG_EX_OPERATORLEEQ ~ rightEv ^^ { case ~(~(var1, op), v) => LEEQ(var1, v) }
  lazy val expressionGR: Parser[Proposition] = leftEv ~ REG_EX_OPERATORGR ~ rightEv ^^ { case ~(~(var1, op), v) => GR(var1, v) }
  lazy val expressionLE: Parser[Proposition] = leftEv ~ REG_EX_OPERATORLE ~ rightEv ^^ { case ~(~(var1, op), v) => LE(var1, v) }

  lazy val leftEv: Parser[StateEvaluator] = uuid(str => SVIDEval(ID.makeID(str).get)) | stringValue
  lazy val rightEv: Parser[StateEvaluator] = uuid(str => SVIDEval(ID.makeID(str).get)) | intValue | trueValue | falseValue | stringValue

  lazy val stringValue = REG_EX_STRINGVALUE ^^ {
    v => spidMap.get(v) match {
      case Some(id) => SVIDEval(id)
      case _ => ValueHolder(StringPrimitive(v))
    }
  }

}

case class ActionParser(idablesToParseFromString: List[IDAble] = List()) extends BaseParser {

  def parseStr(str: String) = parseAll(factor, str) match {
    case Success(result, _) => Right(result)
    case failure: NoSuccess => Left(failure) //failure.toString = "["+failure.next.pos+"] error: "+failure.msg+"\n\n"+failure.next.pos.longString
  }

  final lazy val REG_EX_OPERATORASSIGN = s"=|:=".r
  final lazy val REG_EX_OPERATORINCR = s"+=".r
  final lazy val REG_EX_OPERATORDECR = s"-=".r

  lazy val factor: Parser[Action] = expressionASSIGNtype1 | expressionASSIGNtype2 | expressionINCR | expressionDECR | "(" ~> factor <~ ")" ^^ { case exp => exp }
  lazy val expressionASSIGNtype1: Parser[Action] = leftEv ~ REG_EX_OPERATORASSIGN ~ REG_EX_UUID ^^ { case ~(~(var1, _), v) => Action(var1, ASSIGN(ID.makeID(v).get)) }
  lazy val expressionASSIGNtype2: Parser[Action] = leftEv ~ REG_EX_OPERATORASSIGN ~ rightEv ^^ { case ~(~(var1, _), v) => Action(var1, v) }
  lazy val expressionINCR: Parser[Action] = leftEv ~ REG_EX_OPERATORINCR ~ REG_EX_INTVALUE ^^ { case ~(~(var1, _), v) => Action(var1, INCR(Integer.parseInt(v))) }
  lazy val expressionDECR: Parser[Action] = leftEv ~ REG_EX_OPERATORDECR ~ REG_EX_INTVALUE ^^ { case ~(~(var1, _), v) => Action(var1, DECR(Integer.parseInt(v))) }

  lazy val leftEv: Parser[ID] = uuid(str => ID.makeID(str).get) | stringValue_ID
  lazy val rightEv: Parser[StateUpdater] = intValue | trueValue | falseValue | stringValue

  lazy val stringValue_ID = s"${spidMap.keySet.mkString("|")}".r ^^ { case str => spidMap(str) }
  lazy val stringValue = REG_EX_STRINGVALUE ^^ {
    v => spidMap.get(v) match {
      case Some(id) => ASSIGN(id)
      case _ => ValueHolder(StringPrimitive(v))
    }
  }

}

trait BaseParser extends JavaTokenParsers {
  final lazy val REG_EX_UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r
  final lazy val REG_EX_STRINGVALUE = s"\\w+".r //http://www.autohotkey.com/docs/misc/RegEx-QuickRef.htm
  final lazy val REG_EX_INTVALUE = s"\\d+".r
  final lazy val REG_EX_TRUE = s"true|TRUE|T".r
  final lazy val REG_EX_FALSE = s"false|FALSE|F".r

  def uuid[T](parserFunction: String => T) = REG_EX_UUID ^^ { parserFunction }
  lazy val intValue = REG_EX_INTVALUE ^^ { v => ValueHolder(IntPrimitive(Integer.parseInt(v))) }
  final lazy val trueValue = REG_EX_TRUE ^^ { v => ValueHolder(StringPrimitive("TRUE")) }
  final lazy val falseValue = REG_EX_FALSE ^^ { v => ValueHolder(StringPrimitive("FALSE")) }

  val idablesToParseFromString: List[IDAble]
  lazy val spidMap = idablesToParseFromString.map(item => item.name -> item.id).toMap
}

//object TestParser extends App {
//  println("Welcome to the parser: Type an expression or enter for exit")
//  waitForString
//
//  private def waitForString {
//    def waitEOF(): Unit = Console.readLine() match {
//      case "" =>
//      case "exit" =>
//      case str: String => {
//        println(PropositionParser.parseStr(str))
//        //          println(PropositionParser.parseAll(PropositionParser.stateEv, str))
//        waitEOF()
//      }
//    }
//    waitEOF()
//  }
//}