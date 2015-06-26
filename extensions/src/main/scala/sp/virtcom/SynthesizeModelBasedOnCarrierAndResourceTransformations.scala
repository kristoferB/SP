package sp.virtcom

import akka.actor._
import org.json4s.JsonAST._
import sp.domain.logic.{PropositionConditionLogic, ActionParser}
import sp.domain._
import sp.jsonImporter.ServiceSupportTrait
import sp.supremicaStuff.base._
import sp.system.messages._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import sp.domain.Logic._

/**
 * Creates a wmod module from checked:
 * Operations (with attributes describing "carrierTransformation" and/or "resourceTransformation"
 * SOPSpecs (with operations modeling product refinement or with operations modeling mutex as arbitrary order)
 * Things (variables)
 * This module is synthesized.
 * Creates transport operations for Things with attribute field: "addTransport:true/yes"
 * Creates a Condition for each operation based on:
 * "carrierTransformation"
 * "resourceTransformation"
 * synthesized guard
 * Creates and extends given Things to apply with created wmod module
 */
class SynthesizeModelBasedOnCarrierAndResourceTransformations(modelHandler: ActorRef) extends Actor with ServiceSupportTrait {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(service, attr) =>

      println(s"service: $service")

      lazy val id = attr.getAs[ID]("activeModelID").getOrElse(ID.newID)
      lazy val checkedItems = attr.findObjectsWithField(List(("checked", JBool(true)))).unzip._1.flatMap(ID.makeID)

      val result = for {
        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))

        //Collect ops, vars, sopSpecs
        SPIDs(opsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetOperations(model = modelInfo.model))
        ops = opsToBe.map(_.asInstanceOf[Operation])
        checkOps = ops.filter(obj => checkedItems.contains(obj.id))

        SPIDs(varsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetThings(model = modelInfo.model,
          filter = { obj => checkedItems.contains(obj.id) && obj.isInstanceOf[Thing] }))
        vars = varsToBe.map(_.asInstanceOf[Thing])

        SPIDs(sopSpecsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetSpecs(model = modelInfo.model,
          filter = { obj => checkedItems.contains(obj.id) && obj.isInstanceOf[SOPSpec] }))
        sopSpecs = sopSpecsToBe.map(_.asInstanceOf[SOPSpec])

        //Extend and check SopsWithCarrierTransformation
        eswct = ExtendSopsWithCarrierTransformation(sopSpecs, ops)

        //        //Create Supremica Module and synthesize guards.
        //        ptmw = ParseToModuleWrapper(modelInfo.name, vars, ops, specs)
        //        optSupervisorGuards = {
        //          ptmw.addVariables()
        //          ptmw.saveToWMODFile("./testFiles/gitIgnore/")
        //          ptmw.addOperations()
        //          ptmw.saveToWMODFile("./testFiles/gitIgnore/")
        //          ptmw.addForbiddenExpressions()
        //          ptmw.saveToWMODFile("./testFiles/gitIgnore/")
        //          ptmw.SupervisorAsBDD().getSupervisorGuards.map(_.filter(og => !og._2.equals("1")))
        //        }

        //        //Update operations with conditions and change to Supremica syntax
        //        updatedOps = ops.map(o => ptmw.changeToSupremicaSyntaxAndAddSPCondition(o, optSupervisorGuards))
        _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id, modelVersion = modelInfo.version, items = List()))

      } yield {

          println(checkOps.map(_.name).mkString("\n"))
          println(vars.map(_.name).mkString("\n"))
          println(sopSpecs.map(_.name).mkString("\n"))
          //          optSupervisorGuards.getOrElse(Map()).foreach(kv => println(s"${kv._1}: ${kv._2}"))
          //          //          updatedOps.foreach(o => println(s"${o.name} c:${o.conditions} a:${o.attributes.pretty}"))
          //
          //          ptmw.addSupervisorGuardsToFreshFlower(optSupervisorGuards)
          //          ptmw.saveToWMODFile("./testFiles/gitIgnore/")

        }

      sender ! result
  }

}

object SynthesizeModelBasedOnCarrierAndResourceTransformations {
  def props(modelHandler: ActorRef) = Props(classOf[SynthesizeModelBasedOnCarrierAndResourceTransformations], modelHandler)
}

case class ExtendSopsWithCarrierTransformation(sopSpecs: List[SOPSpec], operations: List[Operation]) {
  lazy val operationIdMap = operations.map(o => o.id -> o).toMap

  sopSpecs.foreach { sopSpec =>
    lazy val productName = sopSpec.name
    sopSpec.sop.foreach { sop => sop match {
      case seq: Sequence =>
        def checkAndOrUpdateCarrierTransformationAttribute(remainingSeq: Seq[SOP], optPreviousAtCompleteValue: Option[String] = None): Unit = {
          remainingSeq match {
            case node +: nodes =>
              node match {
                case h: Hierarchy if operationIdMap.contains(h.operation) =>
                  lazy val op = operationIdMap(h.operation)
                  println(op.name)
//                  op.attributes \ "carrierTrans" transformField {
//
//                  }

                //atStart
                //atComplete
                //atExecute
                case _ => println(s"SOP nbr ${sopSpec.sop.indexOf(sop) + 1} in SOPSpec ${sopSpec.name} has a node that is not an Operation/Hierarchy. This and the remaining nodes in this SOP will be ignored.")
              }
            case Nil => //Do nothing
          }
        }
        checkAndOrUpdateCarrierTransformationAttribute(seq.sop)
      case _ => println(s"SOP nbr ${sopSpec.sop.indexOf(sop) + 1} in SOPSpec ${sopSpec.name} has an outermost SOP that is not a Sequence. This SOP will be ignored.")
    }

    }

  }
}

case class AnotherParseToModuleWrapper(moduleName: String, vars: List[Thing], ops: List[Operation], spec: List[SPSpec]) extends FlowerPopulater with Exporters with Algorithms with TextFilePrefix {

  lazy val variableMap = vars.flatMap(v => {
    val optDomain = v.attributes.findAs[Seq[String]]("domain").headOption
    if (optDomain.isDefined) Some(v.name -> optDomain.get) else None
  }).toMap

  lazy val mModule = SimpleModuleFactory(moduleName)

  private def addTransition(o: Operation, event: String, guard: String, action: String) = {
    val guardAsString = o.attributes.findAs[Set[String]](guard).flatten.mkString("(", ")&(", ")")
    val actionsAsString = o.attributes.findAs[Set[String]](action).flatten
    addLeaf(event, stringPredicateToSupremicaSyntax(guardAsString),
      actionsAsString.map(stringActionToSupremicaSyntax).mkString("; "))
  }

  def addOperations() = {
    ops.foreach { o =>
      //      println(s"${o.name} ${o.attributes.pretty}")
      //pre
      val startEvent = o.name
      addEventIfNeededElseReturnExistingEvent(startEvent, unControllable = false)
      addTransition(o, startEvent, "preGuard", "preAction")

      //post
      val compEvent = s"$UNCONTROLLABLE_PREFIX$startEvent"
      addEventIfNeededElseReturnExistingEvent(compEvent, unControllable = true)
      addTransition(o, compEvent, "postGuard", "postAction")

      //Add operation events  to module comment
      mModule.setComment(s"$getComment$OPERATION_PREFIX${o.name} $TRANSITION_PREFIX$startEvent,$compEvent")
    }
  }

  def addVariables() = {
    vars.foreach { v => for {
      domain <- variableMap.get(v.name)
      init <- v.attributes.findAs[Int]("init").headOption
      markings <- v.attributes.getAs[Set[String]]("markings")
      intMarkings = markings.flatMap(m => getFromVariableDomain(v.name, m, "Problem with marking"))
    } yield {
        addVariable(v.name, 0, domain.size - 1, init, intMarkings)
        //Add variable values to module comment
        mModule.setComment(s"$getComment${TextFilePrefix.VARIABLE_PREFIX}${v.name} d${TextFilePrefix.COLON}${domain.mkString(",")}")
      }
    }
  }

  def addForbiddenExpressions() = {
    spec.foreach { s => for {
      exprs <- s.attributes.findAs[Set[String]]("forbiddenExpressions")
    } yield {
        addForbiddenExpression(
          forbiddenExpression = stringPredicateToSupremicaSyntax(exprs.mkString("(", ")|(", ")")),
          addSelfLoop = false,
          addInComment = true)
      }
    }
  }

  private def getFromVariableDomain(variable: String, value: String, errorMsg: String): Option[Int] = {
    variableMap.get(variable) match {
      case Some(domain) => domain.indexOf(value) match {
        case -1 => println(s"$errorMsg\nValue: $value is not in the domain of variable: $variable. The result will not be correct!"); None
        case other => Some(other)
      }
      case _ => println(s"$errorMsg\nVariable: $variable is not defined. The result will not be correct!"); None

    }
  }

  def changeToSupremicaSyntaxAndAddSPCondition(o: Operation, synthesizedGuardMap: Option[Map[String, String]]): Operation = {
    lazy val updatedAttribute = o.attributes.transformField {
      case ("preGuard", JArray(vs)) => ("preGuard", JArray(vs.map { case JString(x) => JString(stringPredicateToSupremicaSyntax(x)); case other => other }))
      case ("preAction", JArray(vs)) => ("preAction", JArray(vs.map { case JString(x) => JString(stringActionToSupremicaSyntax(x)); case other => other }))
      case ("postGuard", JArray(vs)) => ("postGuard", JArray(vs.map { case JString(x) => JString(stringPredicateToSupremicaSyntax(x)); case other => other }))
      case ("postAction", JArray(vs)) => ("postAction", JArray(vs.map { case JString(x) => JString(stringActionToSupremicaSyntax(x)); case other => other }))
    }

    lazy val updatedAttributeWithSynthesizedGuards = synthesizedGuardMap.get.get(o.name) match {
      case Some(guard) => updatedAttribute.to[SPAttributes].getOrElse(SPAttributes()).transformField {
        case ("preGuard", JArray(vs)) => ("preGuard", JArray(JString(guard) :: vs))
      }
      case _ => updatedAttribute
    }

    val opWithUpdatedAttributes = o.copy(attributes = updatedAttributeWithSynthesizedGuards.to[SPAttributes].getOrElse(SPAttributes()))

    //Method starts
    if (!synthesizedGuardMap.isDefined) o else PropositionConditionLogic.parseAttributesToPropositionCondition(opWithUpdatedAttributes, vars).getOrElse(opWithUpdatedAttributes)
  }

  //To get correct syntax of guards and actions in Supremica
  //Variable values are changed to index in domain

  import sp.domain.logic.PropositionParser
  import sp.domain._

  private def stringActionToSupremicaSyntax(s: String) = ActionParser(vars).parseStr(s) match {
    case Right(a) => actionToSupremicaSyntax(a) match {
      case Some(r) => r
      case _ => a.toString
    }
    case other => other.toString
  }

  private def actionToSupremicaSyntax(a: Action) = {
    val varsIdMap = vars.map(v => v.id -> v.name).toMap
    val value = a.value match {
      case ValueHolder(org.json4s.JString(v)) => v
      case ValueHolder(org.json4s.JInt(v)) => v.toString()
      case other =>
        println(s"actionToSupremicaSyntax cannot handle: $other right now. sorry")
        other.toString
    }
    for {
      variable <- varsIdMap.get(a.id)
    } yield {
      s"$variable=${if (isInt(value)) value else getFromVariableDomain(variable, value, "Problem with action").getOrElse("NONE")}"
    }
  }

  private def stringPredicateToSupremicaSyntax(s: String) = PropositionParser().parseStr(s) match {
    case Right(p) => propToSupremicaSyntax(p)
    case other => other.toString
  }

  private def propToSupremicaSyntax(p: Proposition): String = p match {
    case AND(ps) => ps.map(propToSupremicaSyntax).mkString("(", ")&(", ")")
    case OR(ps) => ps.map(propToSupremicaSyntax).mkString("(", ")|(", ")")
    case NOT(q) => s"!${propToSupremicaSyntax(q)}"
    case EQ(l, r) => leftRight(l, "==", r)
    case NEQ(l, r) => leftRight(l, "!=", r)
    case GREQ(l, r) => leftRight(l, ">=", r)
    case GR(l, r) => leftRight(l, ">", r)
    case LEEQ(l, r) => leftRight(l, "<=", r)
    case LE(l, r) => leftRight(l, "<", r)
    case AlwaysTrue => "1"
    case AlwaysFalse => "0"
    case other =>
      println(s"propToSupremicaSyntax cannot handle: $other right no. sorry")
      other.toString
  }

  private def leftRight(l: StateEvaluator, operator: String, r: StateEvaluator) = {
    val left = stateEvalToSupremicaSyntax(l)
    val right = stateEvalToSupremicaSyntax(r)
    s"$left$operator${if (isInt(right)) right else getFromVariableDomain(left, right, "Problem with guard").getOrElse("NONE")}"
  }

  private def stateEvalToSupremicaSyntax(se: StateEvaluator): String = se match {
    case ValueHolder(org.json4s.JString(v)) => v
    case ValueHolder(org.json4s.JInt(v)) => v.toString()
    case other =>
      println(s"stateEvalToSupremicaSyntax cannot handle: $other right now. sorry")
      other.toString
  }

  private def isInt(s: String): Boolean = {
    try {
      s.toInt
      true
    } catch {
      case e: Exception => false
    }
  }

}