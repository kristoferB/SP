package sp.virtcom

import org.json4s.JsonAST.{JObject, JField, JString}
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
    val guardAction = s.split(";").toSeq
    val guard = if (!guardAction.head.isEmpty) Some(SPAttributes("postGuard" -> guardAction.head)) else None
    val actions = if (guardAction.size > 1) Some(SPAttributes("postAction" -> guardAction.tail.toSet)) else None
    Seq(guard,actions).flatten
  }
  def op(name: String, conditions: Seq[SPAttributes], postConditions: Seq[SPAttributes] = SPAttributes(), attributes: SPAttributes = SPAttributes()) = {
    import sp.domain.logic.AttributeLogic._
    val attrUpdated = (conditions ++ postConditions).foldLeft(attributes) { case (acc, c) => acc + c }
    operationSet += Operation(name = name, attributes = attrUpdated)
  }

  def x(name: String, forbiddenExpressions: Set[String]): Unit = {
    val optExistingFE = forbiddenExpressionMap.get(name)
    forbiddenExpressionMap = forbiddenExpressionMap ++ Map(name -> (optExistingFE.getOrElse(Set()) ++ forbiddenExpressions))
  }

  def c(variable: String, fromValue: String, toValue: String): SPAttributes = SPAttributes("preGuard" -> Set(s"$variable==$fromValue"), "preAction" -> Set(s"$variable=$toValue"))
  def c(variable: String, fromValue: String, inBetweenValue: String, toValue: String): SPAttributes = c(variable, fromValue, inBetweenValue) + SPAttributes("postGuard" -> Set(s"$variable==$inBetweenValue"), "postAction" -> Set(s"$variable=$toValue"))

  def createMoveOperations(robotName: String, staticRobotPoses : Map[String,Set[String]]) = {
    staticRobotPoses.foreach {
      case (source, targets) =>
        targets.foreach { target =>
          val inBetweenValue = s"${source}To${target.capitalize}"
          val robot_pos = s"v${robotName}_pos"
          op(s"${inBetweenValue}_$robotName", c(robot_pos, s"$source", inBetweenValue, s"$target"))
          v(robot_pos, domain = Seq(s"$source", inBetweenValue, s"$target"))
        }

    }
  }

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
      val opsUpdated = ops.map { case (o, attr) =>
        val keyValues = Seq("preGuard", "preAction", "postGuard", "postAction").map(key => key -> attr.findAs[Set[String]](key).flatten.toSet).toMap
        val cleanAttr = attr removeField {
          case ("preGuard", _) => true
          case ("preAction", _) => true
          case ("postGuard", _) => true
          case ("postAction", _) => true
          case _ => false
        }
        val updatedAttr = cleanAttr.getAs[JObject].getOrElse(SPAttributes()) + SPAttributes(
          "preGuard" -> keyValues("preGuard"),
          "preAction" -> keyValues("preAction"),
          "postGuard" -> keyValues("postGuard"),
          "postAction" -> keyValues("postAction")
        )

        o -> updatedAttr
      }
      val opsToAdd = opsUpdated.map(kv => Operation(name = kv._1, attributes = kv._2)).toList
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
