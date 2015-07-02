package sp.virtcom

import org.json4s.JsonAST.{JField, JArray, JObject}
import sp.domain._
import sp.domain.Logic._

/**
 * To store operation models
 */

trait CollectorModel {
  var variableSet: Set[Thing] = Set()
  var operationSet: Set[Operation] = Set()
  var forbiddenExpressionSet: Set[SOPSpec] = Set()
  var robotMovementsSet: Set[SPSpec] = Set()

  def v(name: String, domain: Seq[String] = Seq(), init: Option[String] = None, marked: Set[String] = Set(), idleValue: Option[String] = None) = {
    variableSet += Thing(name = name, attributes = SPAttributes("markings" -> (if (marked.isEmpty) None: Option[Set[String]] else Some(marked)),
      "idleValue" -> idleValue,
      "stateVariable" -> SPAttributes(
        "domain" -> (domain ++ (if (init.isDefined) Seq(init.get) else Seq()) ++ marked.toSeq ++ (if (idleValue.isDefined) Seq(idleValue.get) else Seq())),
        "init" -> init,
        "goal" -> (if (marked.size == 1) marked.head else None: Option[Int])
      )))
  }

  implicit def SPAttToSeqOfSpAtt(spa: SPAttributes): Seq[SPAttributes] = Seq(spa)
  implicit def stringStringToSeqOfSpAtt(kv: (String, String)): SPAttributes = SPAttributes(kv)

  def op(name: String, attributes: Seq[SPAttributes] = Seq(SPAttributes())) = {
    operationSet += Operation(name = name, attributes = attributes.foldLeft(SPAttributes()) { case (acc, c) => acc merge c })
  }

  def c(variable: String, fromValue: String, toValue: String): SPAttributes = {
    SPAttributes("preGuard" -> Set(s"$variable == $fromValue"), "preAction" -> Set(s"$variable = $toValue"))
  }
  def c(variable: String, fromValue: String, inBetweenValue: String, toValue: String): SPAttributes = {
    c(variable, fromValue, inBetweenValue) + SPAttributes("postGuard" -> Set(s"$variable == $inBetweenValue"), "postAction" -> Set(s"$variable = $toValue"))
  }
  def aResourceTrans(resource: String, atStart: String, atExecute: String, atComplete: String) = {
    "resourceTrans" -> SPAttributes(resource -> SPAttributes("atStart" -> atStart, "atExecute" -> atExecute, "atComplete" -> atComplete))
  }
  def aCarrierTrans(carrier: String, atStart: Option[String] = None, atExecute: Option[String] = None, atComplete: Option[String] = None) = {
    "carrierTrans" -> SPAttributes(carrier ->
      SPAttributes("atStart" -> atStart, "atExecute" -> atExecute, "atComplete" -> atComplete)
      //        .foldLeft(Seq(): Seq[(String, String)]) {
      //        case (acc, (key, optValue)) => acc ++ (if (optValue.isDefined) Seq(key -> optValue.get) else Seq())
      //      })
    )
  }

  def x(name: String, forbiddenExpressions: Set[String] = Set(), operations: Set[String] = Set()) = {
    forbiddenExpressionSet += SOPSpec(name = name, sop = List(), attributes = SPAttributes(
      "forbiddenExpressions" -> (if (forbiddenExpressions.isEmpty) None else Some(forbiddenExpressions)),
      "mutexOperations" -> (if (operations.isEmpty) None else Some(operations))))
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

  def robotMovements(robotNamePrefix: String = "v", robotName: String, robotNameSuffix: String = "_pos", staticRobotPoses: SPAttributes) = {
    robotMovementsSet += SPSpec(name = robotName, attributes = SPAttributes(
      "robotNamePrefix" -> robotNamePrefix,
      "robotName" -> robotName,
      "robotNameSuffix" -> robotNameSuffix,
      "staticRobotPoses" -> staticRobotPoses))
  }

  implicit def stringToOption: String => Option[String] = Some(_)
  implicit def stringToSetOfStrings: String => Set[String] = Set(_)
  implicit def stringToSeqOfStrings: String => Seq[String] = Seq(_)
}

/**
 * To work on a collector Model
 */
object CollectorModelImplicits {

  implicit class CollectorModelWorker(cm: CollectorModel) {

    def getIDablesFromSet[T <: IDAble](idableSet: Set[T], constructor: (String, JObject) => T) = {
      lazy val idables = idableSet.groupBy(_.name).map { case (k, objs) => k -> objs.foldLeft(SPAttributes()) { case (acc, obj) => acc merge obj.attributes } }
      idables.map(kv => constructor(kv._1, kv._2)).toList
    }

    def parseToIDables() = {
      //Variables-----------------------------------------------------------------------------------------------------
      lazy val varsToAddWithNonDistinctDomains = getIDablesFromSet(cm.variableSet, (n, as) => Thing(name = n, attributes = as))
      lazy val varsToAdd = varsToAddWithNonDistinctDomains.map { obj => obj.copy(attributes =
        obj.attributes.transformField { case ("domain", JArray(vs)) => ("domain", JArray(vs.distinct)) }.to[SPAttributes].getOrElse(SPAttributes()))
      }

      //Operations------------------------------------------------------------------------------------
      lazy val opsToAdd = getIDablesFromSet(cm.operationSet, (n, as) => Operation(name = n, attributes = as))
      lazy val operationMap = opsToAdd.map(o => o.name -> o).toMap

      //ForbiddenExpressions--------------------------------------------------------------------------------------
      lazy val fesToAddWithNoVisableOperations = getIDablesFromSet(cm.forbiddenExpressionSet, (n, as) => SOPSpec(name = n, sop = List(), attributes = as))
      lazy val fesToAdd = fesToAddWithNoVisableOperations.map { obj => obj.copy(sop =
        List(Arbitrary(obj.attributes.findAs[List[String]]("mutexOperations").flatten.flatMap(o => operationMap.get(o)).map(o => Hierarchy(o.id)): _*)),
        attributes = obj.attributes.removeField { case JField("mutexOperations", _) => true; case _ => false }.to[SPAttributes].getOrElse(SPAttributes()))
      }

      //RobotMovements---------------------------------------------------------------------------------------------
      lazy val robotMovementsToAdd = getIDablesFromSet(cm.robotMovementsSet, (n, as) => SPSpec(name = n, attributes = as))

      //Return--------------------------------------------------------------------------------------------
      varsToAdd ++ opsToAdd ++ fesToAdd ++ robotMovementsToAdd
    }

  }

}
