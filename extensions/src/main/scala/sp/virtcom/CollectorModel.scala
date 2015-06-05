package sp.virtcom

import sp.domain.{SPSpec, Operation, Thing, SPAttributes}
import sp.domain.Logic._

/**
 * To store operation models
 */
trait CollectorModel {
  var variableMap: Map[String, VariableProperties] = Map()
  var operationSet: Set[Operation] = Set()
  var forbiddenExpressionMap: Map[String, Set[String]] = Map()

  def v(name: String, domain: Seq[String] = Seq(), init: Option[String] = None, marked: Set[String] = Set()): Unit = {
    val value = variableMap.getOrElse(name, VariableProperties())
    variableMap += (name -> VariableProperties(value.domain ++ domain ++ marked ++ (if (init.isDefined) Set(init.get) else Set()), if (init.isDefined) init else value.init, value.marked ++ marked))
  }

  implicit def SPAttToSeqOfSpAtt(spa: SPAttributes): Seq[SPAttributes] = Seq(spa)
  implicit def stringToSeqOfSpAtt(s: String): Seq[SPAttributes] = {
    val guardAction = s.split(";")
    val actions: Set[String] = if (guardAction.size > 1) guardAction.drop(1).toSet else Set()
    Seq(SPAttributes("freeTextCondition" -> Map("guard" -> guardAction.head, "actions" -> actions)))
  }
  def op(name: String, conditions: Seq[SPAttributes], postActions: Seq[SPAttributes] = SPAttributes(), attributes: SPAttributes = SPAttributes()) = {
    import sp.domain.logic.AttributeLogic._
    val attrUpdatedWithCond = conditions.foldLeft(attributes) { case (acc, c) => acc + c }
    val attrUpdatedWithPostAction = postActions.foldLeft(attrUpdatedWithCond) { case (acc, pa) => {
      val optAs = pa.findAs[Set[String]]("actions").headOption
      if (optAs.isDefined) optAs.get.foldLeft(acc) { case (innerAcc, a) => innerAcc + SPAttributes("postAction" -> a) } else acc
    }
    }
    operationSet += Operation(name = name, attributes = conditions.foldLeft(attributes) { case (acc, c) => acc + c })
  }

  def x(name: String, forbiddenExpressions: Set[String]): Unit = {
    val optExistingFE = forbiddenExpressionMap.get(name)
    forbiddenExpressionMap = forbiddenExpressionMap ++ Map(name -> (optExistingFE.getOrElse(Set()) ++ forbiddenExpressions))
  }

  def c(variable: String, fromValue: String, toValue: String): SPAttributes = SPAttributes("preGuard" -> s"$variable==$fromValue", "preAction" -> s"$variable=$toValue")
  def c(variable: String, fromValue: String, inBetweenValue: String, toValue: String): SPAttributes = c(variable, fromValue, inBetweenValue) + SPAttributes("postGuard" -> s"$variable==$inBetweenValue", "postAction" -> s"$variable=$toValue")

  implicit def stringToOption: String => Option[String] = Some(_)
  implicit def stringToSetOfStrings: String => Set[String] = Set(_)
  implicit def stringToSeqOfStrings: String => Seq[String] = Seq(_)
}

case class VariableProperties(domain: Seq[String] = Seq(), init: Option[String] = None, marked: Set[String] = Set())

/**
 * To work on a collector Model
 */
object CollectorModelImplicits {

  implicit class CollectorModelWorker(cm: CollectorModel) {

    def parseToIDables() = {
      cm.variableMap = cm.variableMap.map { case (name, vp) => name -> vp.copy(vp.domain.distinct) }
      //Variables-----------------------------------------------------------------------------------------------------
      val varsToAdd = cm.variableMap.flatMap { case (name, vp) =>
        for {
          initValue <- getFromVariableDomain(name, vp.init.getOrElse("NONE"), "Problem with init value")
        } yield {
          Thing(name = name, attributes = SPAttributes("markings" -> vp.marked,
            "stateVariable" -> Map(
              "domain" -> vp.domain,
              "init" -> initValue,
              "goal" -> (if (vp.marked.size == 1) getFromVariableDomain(name, vp.marked.head, "Problem with marking") else None: Option[Int])
            )))
        }
      }.toList

      //Operations------------------------------------------------------------------------------------
      val ops = cm.operationSet.groupBy(_.name).map { case (k, os) => k -> os.foldLeft(SPAttributes()) { case (acc, o) => acc + o.attributes } }
      val opsToAdd = ops.map(kv => Operation(name = kv._1, attributes = kv._2)).toList
      //      opsToAdd.foreach(o => println(s"n:${o.name} c:${o.conditions} a:${o.attributes.pretty}"))

      //ForbiddenExpressions--------------------------------------------------------------------------------------
      val fesToAdd = cm.forbiddenExpressionMap.map { case (name, fes) =>
        SPSpec(name = name, attributes = SPAttributes("forbiddenExpressions" -> fes))
      }.toList

      //Return--------------------------------------------------------------------------------------------
      varsToAdd ++ opsToAdd ++ fesToAdd
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

  }

}
