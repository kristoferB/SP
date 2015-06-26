package sp.virtcom

import sp.domain._
import sp.domain.Logic._

/**
 * To store operation models
 */
trait CollectorModel {
  var variableMap: Map[String, VariableProperties] = Map()
  var operationSet: Set[Operation] = Set()
  var forbiddenExpressionMap: Map[String, ForbiddenExpressionInfo] = Map()
  var productSpecMap: Map[String, Seq[String]] = Map()

  def v(name: String, domain: Seq[String] = Seq(), init: Option[String] = None, marked: Set[String] = Set(), idleValue: Option[String] = None): Unit = {
    val value = variableMap.getOrElse(name, VariableProperties())
    variableMap += (name -> VariableProperties(value.domain ++ domain ++ marked ++ (if (init.isDefined) Set(init.get) else Set()), if (init.isDefined) init else value.init, value.marked ++ marked, if (idleValue.isDefined) idleValue else value.idleValue))
  }

  implicit def SPAttToSeqOfSpAtt(spa: SPAttributes): Seq[SPAttributes] = Seq(spa)
  implicit def stringToSeqOfSpAtt(s: String): Seq[SPAttributes] = {
    val guardAction = s.split(";").toSeq
    val guard = if (!guardAction.head.isEmpty) Some(SPAttributes("postGuard" -> Set(guardAction.head))) else None
    val actions = if (guardAction.size > 1) Some(SPAttributes("postAction" -> guardAction.tail.toSet)) else None
    Seq(guard, actions).flatten
  }
  def op(name: String, conditions: Seq[SPAttributes] = SPAttributes(), postConditions: Seq[SPAttributes] = SPAttributes(), attributes: SPAttributes = SPAttributes()) = {
    import sp.domain.logic.AttributeLogic._
    val attrUpdated = (conditions ++ postConditions).foldLeft(attributes) { case (acc, c) => acc merge c }
    operationSet += Operation(name = name, attributes = attrUpdated)
  }

  def c(variable: String, fromValue: String, toValue: String): SPAttributes = SPAttributes("preGuard" -> Set(s"$variable == $fromValue"), "preAction" -> Set(s"$variable = $toValue"))
  def c(variable: String, fromValue: String, inBetweenValue: String, toValue: String): SPAttributes = c(variable, fromValue, inBetweenValue) + SPAttributes("postGuard" -> Set(s"$variable == $inBetweenValue"), "postAction" -> Set(s"$variable = $toValue"))

  def x(name: String, forbiddenExpressions: Set[String] = Set(), operations: Set[String] = Set()): Unit = {
    val info = forbiddenExpressionMap.getOrElse(name, ForbiddenExpressionInfo())
    forbiddenExpressionMap = forbiddenExpressionMap ++ Map(name -> info.copy(expressions = info.expressions ++ forbiddenExpressions, operations = info.operations ++ operations))
  }

  def product(name: String, operationSeq: Seq[String]) = {
    productSpecMap = productSpecMap ++ Map(name -> operationSeq)
  }

  def createMoveOperations(robotNamePrefix: String = "v", robotName: String, robotNameSuffix: String = "_pos", staticRobotPoses: Map[String, Set[String]]) = {
    staticRobotPoses.foreach {
      case (source, targets) =>
        targets.foreach { target =>
          val inBetweenValue = s"${source}To${target.capitalize}"
          val robot_pos = s"$robotNamePrefix$robotName$robotNameSuffix"
          op(s"${inBetweenValue}_$robotName", c(robot_pos, s"$source", inBetweenValue, s"$target"))
          v(robot_pos, domain = Seq(s"$source", inBetweenValue, s"$target"))
        }

    }
  }

  def aResourceTrans(resource: String, atStart: String, atExecute: String, atComplete: String) = {
    "resourceTrans" -> Seq(resource -> Seq("atStart" -> atStart, "atExecute" -> atExecute, "atComplete" -> atComplete))
  }
  def aCarrierTrans(carrier: String, atStart: Option[String] = None, atExecute: Option[String] = None, atComplete: Option[String] = None) = {
    "carrierTrans" -> Seq(carrier ->
      Seq("atStart" -> atStart, "atExecute" -> atExecute, "atComplete" -> atComplete).foldLeft(Seq(): Seq[(String, String)]) {
        case (acc, (key, optValue)) => acc ++ (if (optValue.isDefined) Seq(key -> optValue.get) else Seq())
      })
  }

  implicit def stringToOption: String => Option[String] = Some(_)
  implicit def stringToSetOfStrings: String => Set[String] = Set(_)
  implicit def stringToSeqOfStrings: String => Seq[String] = Seq(_)
}

protected case class ForbiddenExpressionInfo(expressions: Set[String] = Set(), operations: Set[String] = Set())

case class VariableProperties(domain: Seq[String] = Seq(), init: Option[String] = None, marked: Set[String] = Set(), idleValue: Option[String] = None)

/**
 * To work on a collector Model
 */
object CollectorModelImplicits {

  implicit class CollectorModelWorker(cm: CollectorModel) {

    def parseToIDables() = {
      cm.variableMap = cm.variableMap.map { case (name, vp) => name -> vp.copy(vp.domain.distinct) }
      //Variables-----------------------------------------------------------------------------------------------------
      val varsToAdd = cm.variableMap.map { case (name, vp) =>
        Thing(name = name, attributes = SPAttributes("markings" -> vp.marked,
          "idleValue" -> vp.idleValue,
          "stateVariable" -> Map(
            "domain" -> vp.domain,
            "init" -> getFromVariableDomain(name, vp.init.getOrElse("NONE"), "Problem with init value"),
            "goal" -> (if (vp.marked.size == 1) getFromVariableDomain(name, vp.marked.head, "Problem with marking") else None: Option[Int])
          )))
      }.toList

      //Operations------------------------------------------------------------------------------------
      val ops = cm.operationSet.groupBy(_.name).map { case (k, os) => k -> os.foldLeft(SPAttributes()) { case (acc, o) => acc merge o.attributes } }
      val opsToAdd = ops.map(kv => Operation(name = kv._1, attributes = kv._2)).toList
      //      opsToAdd.foreach(o => println(s"n:${o.name} c:${o.conditions} a:${o.attributes.pretty}"))
      lazy val operationMap = opsToAdd.map(o => o.name -> o).toMap

      //ForbiddenExpressions--------------------------------------------------------------------------------------
      val fesToAdd = cm.forbiddenExpressionMap.flatMap { case (name, info) =>
        (if (info.expressions.isEmpty) Seq() else Seq(SPSpec(name = name, attributes = SPAttributes("forbiddenExpressions" -> info.expressions)))) ++
          (if (info.operations.isEmpty) Seq() else Seq(SOPSpec(name = name, sop = List(Arbitrary(info.operations.flatMap(o => operationMap.get(o)).map(o => Hierarchy(o.id, List())).toSeq: _*)))))
      }.toList

      //Products--------------------------------------------------------------------------------------------
      val productsToAdd = cm.productSpecMap.map { case (name, opSeq) =>
        SOPSpec(name = name, sop = List(Sequence(opSeq.flatMap(o => operationMap.get(o)).map(o => Hierarchy(o.id, List())): _*)))
      }.toList

      //Return--------------------------------------------------------------------------------------------
      varsToAdd ++ opsToAdd ++ fesToAdd ++ productsToAdd
    }

    private def getFromVariableDomain(variable: String, value: String, errorMsg: String): Option[Int] = {
      cm.variableMap.get(variable) match {
        case Some(vp) => vp.domain.indexOf(value) match {
          case -1 => println(s"$errorMsg\nValue: $value is not in the domain of variable: $variable. The result might not be correct!"); None
          case other => Some(other)
        }
        case _ => println(s"$errorMsg\nVariable: $variable is not defined. The result might not be correct!"); None

      }
    }

  }

}
