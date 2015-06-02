package sp.virtcom

import base.{FlowerPopulater, SimpleModuleFactory, Exporters, TextFilePrefix}
import sp.domain.logic.PropositionParser

import scala.util.parsing.combinator.RegexParsers

/**
 * To store operation models
 */
trait CollectorModel extends Implicits with RegexParsers with TextFilePrefix {
  var variableMap: Map[String, VariableProperties] = Map()
  var transitionSeq: Seq[String] = Seq()
  var transitionSet: Set[TransitionCM] = Set()
  var forbiddenExpressionSeq: Seq[String] = Seq()

  case class TransitionCM(label: String, guards: Set[String] = Set(), actions: Map[String, String] = Map(), comment: Option[String] = None)

  def t(label: String, conditions: Seq[String], comment: Option[String] = None): Unit = {
    val parsedCondition = conditions.map(parseCondition).flatten.foldLeft((Set(): Set[String], Map(): Map[String, String])) { case (acc, (optGuard, actionMap)) =>
      (acc._1 ++ (if (optGuard.isDefined) Set(optGuard.get) else Set()), actionMap ++ acc._2)
    }
    transitionSet += TransitionCM(label, parsedCondition._1, parsedCondition._2, comment)
    transitionSeq = (s"t:$label\t${conditions.map("c:" + _).mkString("\t")} ${if (comment.isDefined) s"//${comment.get}" else ""}") +: transitionSeq
  }

  def v(name: String, domain: Seq[String] = Seq(), init: Option[String] = None, marked: Set[String] = Set()): Unit = {
    val value = variableMap.getOrElse(name, VariableProperties())
    variableMap += (name -> VariableProperties(value.domain ++ domain ++ marked ++ (if (init.isDefined) Set(init.get) else Set()), if (init.isDefined) init else value.init, value.marked ++ marked))
  }
  def op(name: String, preConditions: Seq[String], postActions: Seq[String]): Unit = {
    t(name, preConditions, Some(s"${TextFilePrefix.OPERATION_PREFIX}$name t${TextFilePrefix.COLON}$name,${UNCONTROLLABLE_PREFIX}$name")) //start of operation
    val actionMap = preConditions.map(parseCondition).flatten.foldLeft(Map(): Map[String, String]) { case (acc, (_, m)) => acc ++ m } //pre actions become post guards
    t(s"${UNCONTROLLABLE_PREFIX}$name", postActions ++ actionMap.map { case (variable, value) => s"$variable==$value" })
  }

  def op(name: String, prePostCondition: (String, String), preConditions: Seq[String] = Seq()): Unit = op(name, prePostCondition._1 +: preConditions, prePostCondition._2)

  def x(name: String, forbiddenExpressions: Seq[String]): Unit = {
    forbiddenExpressionSeq = forbiddenExpressions.map(expr => s"x:$name\t$expr") ++ forbiddenExpressionSeq
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

case class VariableProperties(domain: Seq[String] = Seq(), init: Option[String] = None, marked: Set[String] = Set()) {
  override def toString = "d:" + domain.distinct.mkString(",") + " i:" + init.getOrElse("NONE") + " m:" + (if (marked.isEmpty) "NONE" else marked.mkString(","))
}

/**
 * To work on a collector Model
 */
object SupervisorImplicits {

  implicit class CollectorModelWorker(cm: CollectorModel) extends Exporters with FlowerPopulater {

    lazy val mModule = SimpleModuleFactory("module1")

    def createWmodFile(filePath: String) = {
      parseToModule()
      saveToWMODFile(filePath)
    }

    private def parseToModule() = {
      //Variables
      cm.variableMap = cm.variableMap.map{case (name,vp) => name -> vp.copy(vp.domain.distinct)}
      cm.variableMap.foreach { case (name, vp) =>
        addVariable(name, 0, vp.domain.size - 1, vp.domain.indexOf(vp.init.getOrElse("NONE")), vp.marked.map(vp.domain.indexOf(_)))
      }
      //Transitions
      cm.transitionSet.foreach { t =>
        val propGuards = t.guards.map(PropositionParser().parseStr(_))
      }
    }

  }

}
