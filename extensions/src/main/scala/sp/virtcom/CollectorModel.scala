package sp.virtcom

import sp.supremicaStuff.base.{SimpleModuleFactory, FlowerPopulater, Exporters, TextFilePrefix}

import scala.util.parsing.combinator.RegexParsers

/**
 * To store operation models
 */
trait CollectorModel extends Implicits with RegexParsers with TextFilePrefix {
  var variableMap: Map[String, VariableProperties] = Map()
  var transitionSet: Set[TransitionCM] = Set()
  var forbiddenExpressionMap: Map[String, Set[String]] = Map()

  case class TransitionCM(label: String, guards: Set[String] = Set(), actions: Map[String, String] = Map(), comment: Option[String] = None)

  def t(label: String, conditions: Seq[String], comment: Option[String] = None): Unit = {
    val parsedCondition = conditions.map(parseCondition).flatten.foldLeft((Set(): Set[String], Map(): Map[String, String])) { case (acc, (optGuard, actionMap)) =>
      (acc._1 ++ (if (optGuard.isDefined) Set(optGuard.get) else Set()), actionMap ++ acc._2)
    }
    transitionSet += TransitionCM(label, parsedCondition._1, parsedCondition._2, comment)
  }

  def v(name: String, domain: Seq[String] = Seq(), init: Option[String] = None, marked: Set[String] = Set()): Unit = {
    val value = variableMap.getOrElse(name, VariableProperties())
    variableMap += (name -> VariableProperties(value.domain ++ domain ++ marked ++ (if (init.isDefined) Set(init.get) else Set()), if (init.isDefined) init else value.init, value.marked ++ marked))
  }
  def op(name: String, preConditions: Seq[String], postActions: Seq[String]): Unit = {
    t(name, preConditions, Some(s"${OPERATION_PREFIX}$name t${COLON}$name,${UNCONTROLLABLE_PREFIX}$name")) //start of operation
    val actionMap = preConditions.map(parseCondition).flatten.foldLeft(Map(): Map[String, String]) { case (acc, (_, m)) => acc ++ m } //pre actions become post guards
    t(s"${UNCONTROLLABLE_PREFIX}$name", postActions ++ actionMap.map { case (variable, value) => s"$variable==$value" })
  }

  def op(name: String, prePostCondition: (String, String), preConditions: Seq[String] = Seq()): Unit = op(name, prePostCondition._1 +: preConditions, prePostCondition._2)

  def x(name: String, forbiddenExpressions: Set[String]): Unit = {
    val optExistingFE = forbiddenExpressionMap.get(name)
    forbiddenExpressionMap = forbiddenExpressionMap ++ Map(name -> (optExistingFE.getOrElse(Set()) ++ forbiddenExpressions))
  }

  def c(variable: String, fromValue: String, toValue: String): String = s"$variable==$fromValue; $variable=$toValue"
  def c(variable: String, fromValue: String, inBetweenValue: String, toValue: String): (String, String) = (c(variable, fromValue, inBetweenValue), s"; $variable = $toValue")

  def guardPattern = s"[^;]+".r
  def actionPattern = (s";".r ~> s"[^=]+".r) ~ (s"=".r ~> s"[^=]+".r)
  def parseCondition(str: String) = parse(((guardPattern ?) ~ (actionPattern *)), str.replaceAll(" ", "")) match {
    case Success(~(guard, actions), _) => Some(guard, actions.map { case (~(variable, value)) => variable -> value }.toMap)
    case _ => None
  }

}

trait Implicits {
  //s to Some(s) for example used in variable constructors
  implicit def stringToOption: String => Option[String] = Some(_)
  //s to Set(s) for example used in variable constructors
  implicit def stringToSetOfStrings: String => Set[String] = Set(_)
  //s to Seq(s) for example used in methods to construct operations
  implicit def stringToSeqOfStrings: String => Seq[String] = Seq(_)
}

case class VariableProperties(domain: Seq[String] = Seq(), init: Option[String] = None, marked: Set[String] = Set())

/**
 * To work on a collector Model
 */
object SupervisorImplicits {

  implicit class CollectorModelWorker(cm: CollectorModel) extends Exporters with FlowerPopulater with TextFilePrefix {

    lazy val mModule = SimpleModuleFactory("module1")

    def createModel(filePath: String) = {
      parseToModule()
      addSupervisorGuardsToFreshFlower
//      saveToJSON(s"$filePath${mModule.getName}.json")
    }

    private def parseToModule() = {
      cm.variableMap = cm.variableMap.map { case (name, vp) => name -> vp.copy(vp.domain.distinct) }
      //Variables-----------------------------------------------------------------------------------------------------
      cm.variableMap.foreach { case (name, vp) =>
        for {
          initValue <- getFromVariableDomain(name, vp.init.getOrElse("NONE"), "Problem with init value")
        } yield {
          addVariable(name, 0, vp.domain.size - 1, initValue, vp.marked.map(vp.domain.indexOf(_)))
          mModule.setComment(s"$getComment${VARIABLE_PREFIX}$name d${COLON}${vp.domain.mkString(",")}")
        }
      }
      //Transitions-----------------------------------------------------------------------------------
      cm.transitionSet.foreach { t =>
        val propGuards = t.guards.map(stringPredicateToSupremicaSyntax)
        addEventIfNeededElseReturnExistingEvent(t.label, t.label.startsWith(UNCONTROLLABLE_PREFIX))
        val optAction = t.actions.map { case (k, v) =>
          for {
            value <- getFromVariableDomain(k, v, s"Problem with action for transition: ${t.label}")
          } yield {
            s"$k=$value"
          }
        }.flatten
        addLeaf(t.label, propGuards.mkString("(", ")&(", ")"), optAction.mkString(";"))
        if (t.comment.isDefined) mModule.setComment(s"$getComment${t.comment.get}")
      }
      //ForbiddenExpressions--------------------------------------------------------------------------------------
      cm.forbiddenExpressionMap.values.foreach { fes =>
        val updatedExpr = fes.map(stringPredicateToSupremicaSyntax)
        addForbiddenExpression(updatedExpr.mkString("(", ")|(", ")"), addSelfLoop = false, addInComment = true)
      }
    }

    private def getFromVariableDomain(variable: String, value: String, errorMsg: String): Option[Int] = {
      cm.variableMap.get(variable) match {
        case Some(vp) => vp.domain.indexOf(value) match {
          case -1 => println(s"$errorMsg\nValue: $value is not in the domain of variable: $variable. The result will not be correct!"); None
          case other => Some(other)
        }
        case _ => println(s"$errorMsg\nVariable: $variable is not defined. The result will not be correct!"); None

      }
    }

    //To get correct syntax of guards in Supremica
    //Variable values are changed to index in domain

    import sp.domain.logic.PropositionParser
    import sp.domain._

    private def stringPredicateToSupremicaSyntax(s: String) = PropositionParser().parseStr(s) match {
      case Right(p) => propToSupremicaSyntax(p)
      case other => other.toString
    }

    private def leftRight(l: StateEvaluator, operator: String, r: StateEvaluator) = {
      val left = stateEvalToSupremicaSyntax(l)
      val right = stateEvalToSupremicaSyntax(r)
      val result = for {
        value <- getFromVariableDomain(left,right,"Problem with guard")
      } yield {
        s"$left$operator$value"
      }
      result.getOrElse("NONE")
    }
    private def propToSupremicaSyntax(p: Proposition): String = p match {
      case AND(ps) => ps.map(propToSupremicaSyntax).mkString("(", ")&(", ")")
      case OR(ps) => ps.map(propToSupremicaSyntax).mkString("(", ")|(", ")")
      case NOT(p) => s"!${propToSupremicaSyntax(p)}"
      case EQ(l, r) => leftRight(l, "==", r)
      case NEQ(l, r) => leftRight(l, "!=", r)
      case GREQ(l, r) => leftRight(l, ">=", r)
      case GR(l, r) => leftRight(l, ">", r)
      case LEEQ(l, r) => leftRight(l, "<=", r)
      case LE(l, r) => leftRight(l, "<", r)
      case other => other.toString
    }
    private def stateEvalToSupremicaSyntax(se: StateEvaluator): String = se match {
      case ValueHolder(org.json4s.JString(v)) => v
      case other => other.toString
    }

  }

}
