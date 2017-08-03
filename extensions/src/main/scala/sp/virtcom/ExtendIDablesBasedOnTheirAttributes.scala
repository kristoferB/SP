package sp.virtcom

import akka.actor._
import org.json4s.JsonAST.JArray
import sp.domain._
import sp.services.AddHierarchies
import sp.system.{ServiceSupport, ServiceLauncher}
import sp.system.messages._
import sp.domain.Logic._

/**
 * Extends IDables
 * Operations:
 * "carrierTrans" attributes are extended to get a value keys "atStart", "atExecute", "atComplete" if no exists.
 * E.g. If "atStart":"gripper" and no values are given for "atExecute" and "atComplete" then "atExecute":"partlyGripper" and "atComplete":"empty"
 * SPSpec:
 * "staticRobotPoses" attributes are extended to generate new (transport) operations
 * E.g. {"atHome":{{"to":"atFixture"},{"to":"atTable","simop":"1,202"}}} will generate two operations:
 * "atHomeToAtFixture" and "atHomeToAtTable" (with an attribute "simop":"1,202")
 * Variables:
 * Non-existing variables referenced in attributes of operations are created.
 * The domains for new and old variables are extended based on values given in attributes of operations.
 * Variables without an attribute key "idleValue" that contains the value "empty" in its domain are extend with the attribute: "idleValue":"empty"
 *
 * TODO: Extend based on product SOPs
 */
object ExtendIDablesBasedOnTheirAttributes {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Extend idables based on their attributes"
    )
  )

  def props = ServiceLauncher.props(Props(classOf[ExtendIDablesBasedOnTheirAttributesService]))

}

class ExtendIDablesBasedOnTheirAttributesService extends Actor with ServiceSupport with AddHierarchies {

  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) =>

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      println(s"service: $service")

      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation])
      val vars = ids.filter(_.isInstanceOf[Thing]).map(_.asInstanceOf[Thing])
      val sopSpecs = ids.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec])
      val spSpecs = ids.filter(_.isInstanceOf[SPSpec]).map(_.asInstanceOf[SPSpec])
   // println("vars  ExtendeIDs :::::"     +  vars)
     // println("spSpecs  ::::: "   + spSpecs)

      //Extend Operations and Variables (TODO extend based on product sequences)
      val eiw = ExtendIDablesWrapper(ops, vars, sopSpecs, spSpecs)
      val updatedIDables = {
        eiw.extend()
        eiw.extendedIDables()
      }

      rnr.reply ! Response(updatedIDables, SPAttributes("info" -> "IDAbles extended based on their attributes"), service, reqID)
      self ! PoisonPill

    case (r: Response, reply: ActorRef) => {
      reply ! r
    }
    case x => {
      sender() ! SPError("What should I do? " + x)
      self ! PoisonPill
    }

  }
}

case class TransformationPatternInAttributes(atStart: Option[String], atExecute: Option[String], atComplete: Option[String]) {
  def partlyAtStart() = partly(atStart)
  def partlyAtComplete() = partly(atComplete)
  def betweenStartAndComplete() = for {
    start <- atStart
    complete <- atComplete
  } yield {
      s"${start}To${complete.capitalize}"
    }
  private def partly(optValue: Option[String]) = optValue.map(value => s"partly${value.capitalize}")
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
            "atExecute" -> (if (tpia.atExecute.isDefined) tpia.atExecute else if (tpia.atComplete.isDefined && tpia.atStart.isDefined) tpia.betweenStartAndComplete() else if (tpia.atComplete.isDefined) tpia.partlyAtComplete() else tpia.partlyAtStart()),
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
