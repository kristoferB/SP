package sp.virtcom

import akka.actor._
import org.json4s.JsonAST.{JArray, JBool}
import sp.domain._
import sp.jsonImporter.ServiceSupportTrait
import sp.system.messages._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import sp.domain.Logic._

/**
 * Extends IDables
 * Operations:
 *  "carrierTrans" attributes are extended to get a value keys "atStart", "atExecute", "atComplete" if no exists.
 *  E.g. If "atStart":"gripper" and no values are given for "atExecute" and "atComplete" then "atExecute":"partlyGripper" and "atComplete":"empty"
 * SPSpec:
 *  "staticRobotPoses" attributes are extended to generate new (transport) operations
 *  E.g. {"atHome":{{"to":"atFixture"},{"to":"atTable","simop":"1,202"}}} will generate two operations:
 *   "atHomeToAtFixture" and "atHomeToAtTable" (with an attribute "simop":"1,202")
 * Variables:
 *  Non-existing variables referenced in attributes of operations are created.
 *  The domains for new and old variables are extended based on values given in attributes of operations.
 *  Variables without an attribute key "idleValue" that contains the value "empty" in its domain are extend with the attribute: "idleValue":"empty"
 *
 * TODO: Extend based on product SOPs
 */
class ExtendIDablesBasedOnTheirAttributes(modelHandler: ActorRef) extends Actor with ServiceSupportTrait {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(service, attr,_) =>

      println(s"service: $service")

      lazy val id = attr.getAs[ID]("activeModelID").getOrElse(ID.newID)
            lazy val checkedItems = attr.findObjectsWithField(List(("checked", JBool(true)))).unzip._1.flatMap(ID.makeID)

      val result = for {
        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))

        //Collect ops, vars, sopSpecs, spSpecs
        SPIDs(opsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetOperations(model = modelInfo.id))
        ops = opsToBe.map(_.asInstanceOf[Operation])
        //        checkOps = ops.filter(obj => checkedItems.contains(obj.id))

        SPIDs(varsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetThings(model = modelInfo.id))
        vars = varsToBe.map(_.asInstanceOf[Thing])

        SPIDs(sopSpecsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetSpecs(model = modelInfo.id))
        sopSpecs = sopSpecsToBe.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec])

        SPIDs(spSpecsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetSpecs(model = modelInfo.id))
        spSpecs = spSpecsToBe.filter(_.isInstanceOf[SPSpec]).map(_.asInstanceOf[SPSpec])

        //Extend Operations and Variables (TODO extend based on product sequences)
        eiw = ExtendIDablesWrapper(ops, vars, sopSpecs, spSpecs)
        updatedIDables = {
          eiw.extend()
          eiw.extendedIDables()
        }
        _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id, items = updatedIDables, info = SPAttributes("info" -> "Model extended based on attributes")))

      } yield {

        }

      sender ! result
  }

}

object ExtendIDablesBasedOnTheirAttributes {
  def props(modelHandler: ActorRef) = Props(classOf[ExtendIDablesBasedOnTheirAttributes], modelHandler)
}

case class TransformationPatternInAttributes(atStart: Option[String], atExecute: Option[String], atComplete: Option[String]) {
  private def partly(optValue: Option[String]) = optValue.map(value => s"partly${value.capitalize}")
  def partlyAtStart() = partly(atStart)
  def partlyAtComplete() = partly(atComplete)
  def valuesForDomain() = Seq(atStart, atExecute, atComplete).flatten
}

case class StaticRobotPosesInfoInAttributes(to: String, simop: Option[String])

private case class ExtendIDablesWrapper(var ops: List[Operation], var vars: List[Thing], var sopSpecs: List[SOPSpec], var spSpecs: List[SPSpec]) {

  def extendedIDables() = ops ++ vars ++ sopSpecs

  def extend() = {
    extendOpsWithValuesForAttributeCarrierTrans()
    addTransportOperations()
    addVariablesAndUpdateDomainsForExistingVariables()
  }

  //Extend ops with values for attribute: carrierTrans
  def extendOpsWithValuesForAttributeCarrierTrans() = {
    ops = ops.map { o =>
      lazy val attributeKey = "carrierTrans"

      lazy val updatedAttr = o.attributes.getAs[SPAttributes](attributeKey).map {
        _.obj.map { case (key, toTpia) =>
          lazy val tpia = toTpia.to[TransformationPatternInAttributes].get
          SPAttributes(key -> SPAttributes("atStart" -> tpia.atStart.getOrElse("empty"),
            "atExecute" -> (if (tpia.atExecute.isDefined) tpia.atExecute else if (tpia.atComplete.isDefined) tpia.partlyAtComplete() else tpia.partlyAtStart()),
            "atComplete" -> tpia.atComplete.getOrElse("empty")))
        }
      }.map(_.foldLeft(SPAttributes()) { case (acc, attr) => acc merge attr })

      o.copy(attributes = (o.attributes transformField { case (`attributeKey`, _) => (attributeKey, SPValue(updatedAttr)) }).to[SPAttributes].getOrElse(SPAttributes()))
    }
  }

  //Robot movements
  def addTransportOperations() = {
    lazy val operationMap = ops.map(o => o.name -> o).toMap
    spSpecs.foreach { obj =>
      lazy val transportOps = for {
        robotNamePrefix <- obj.attributes.getAs[String]("robotNamePrefix")
        robotName <- obj.attributes.getAs[String]("robotName")
        robotNameSuffix <- obj.attributes.getAs[String]("robotNameSuffix")
        staticRobotPoses <- obj.attributes.getAs[SPAttributes]("staticRobotPoses")
      } yield {
          staticRobotPoses.obj.flatMap { case (from, toSrpiia) =>
            toSrpiia.to[List[StaticRobotPosesInfoInAttributes]].map {
              _.map { srpiia =>
                lazy val inBetweenValue = s"${from}To${srpiia.to.capitalize}"
                lazy val robot_pos = s"$robotNamePrefix$robotName$robotNameSuffix"
                lazy val operationName = s"${inBetweenValue}_$robotName"
                lazy val attr = SPAttributes("resourceTrans" -> SPAttributes(robot_pos -> SPAttributes("atStart" -> from, "atExecute" -> inBetweenValue, "atComplete" -> srpiia.to)),
                  "simop" -> srpiia.simop)

                operationMap.get(operationName) match {
                  case Some(tOp) => tOp.copy(attributes = tOp.attributes merge attr)
                  case _ => Operation(name = s"${inBetweenValue}_$robotName", attributes = attr.addTimeStamp)
                }
              }
            }
          }
        }
      transportOps.foreach(tOps => ops = ops ++ tOps.flatten)
    }
  }

  //Add variables and update domains. Set default init/marked values.
  def addVariablesAndUpdateDomainsForExistingVariables() = {
    lazy val variableMap = vars.map(o => o.name -> o).toMap

    lazy val variableValueFromOpsMap = ops.flatMap { o =>
      Set("carrierTrans", "resourceTrans").flatMap { transType =>
        o.attributes.getAs[SPAttributes](transType).map {
          _.obj.flatMap { case (variable, toTpia) =>
            toTpia.to[TransformationPatternInAttributes].map { tpia =>
              variable -> SPAttributes("stateVariable" -> SPAttributes("domain" -> tpia.valuesForDomain()))
            }
          }
        }
      }
    }.flatten.groupBy(kv => kv._1).map(kv => kv._1 -> kv._2.unzip._2.foldLeft(SPAttributes()) { case (acc, attr) => acc merge attr })

    vars = variableValueFromOpsMap.map {
      case (variable, attr) => variableMap.get(variable) match {
        case Some(thing) => thing.copy(attributes = thing.attributes merge attr)
        case _ => Thing(name = variable, attributes = attr.addTimeStamp)
      }
    }.map {
      obj => obj.copy(attributes = obj.attributes.transformField { case ("domain", JArray(vs)) => ("domain", JArray(vs.distinct)) }.to[SPAttributes].getOrElse(SPAttributes()))
    }.map {
      obj => obj.attributes.getAs[String]("idleValue") match {
        case Some(_) => obj //Do nothing
        case _ => obj.attributes.findAs[List[String]]("domain").headOption match {
          case Some(domainList) => if (domainList.contains("empty")) obj.copy(attributes = obj.attributes merge SPAttributes("idleValue" -> "empty")) else obj
          case _ => obj //Do nothing
        }
      }
    }.toList
  }
}
