package sp.virtcom

import akka.actor._
import org.json4s.JsonAST.JBool
import sp.domain.logic.{PropositionParser, ActionParser}
import sp.domain._
import sp.jsonImporter.ServiceSupportTrait
import sp.supremicaStuff.base._
import sp.system.messages._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import sp.domain.Logic._

/**
 * Creates a wmod module from existing operations, things (variables), and SOPspec (forbidden expressions/mutex operations).
 * This module is synthesized.
 * The synthesized supervisor is returned as an extra preGuard for a subset of the operations.
 * Creates a Condition for each operation based on its attributes
 */
class SynthesizeModelBasedOnAttributesService(modelHandler: ActorRef) extends Actor with ServiceSupportTrait {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(service, attr) =>

      println(s"service: $service")

      val id = attr.getAs[ID]("activeModelID").getOrElse(ID.newID)
      lazy val checkedItems = attr.findObjectsWithField(List(("checked", JBool(true)))).unzip._1.flatMap(ID.makeID)

      val result = for {
        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))

        //Collect ops, vars, forbidden expressions
        SPIDs(opsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetOperations(model = modelInfo.model))
        //        ops = opsToBe.filter(obj => checkedItems.contains(obj.id)).map(_.asInstanceOf[Operation])
        ops = opsToBe.map(_.asInstanceOf[Operation])
        SPIDs(varsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetThings(model = modelInfo.model))
        //        vars = varsToBe.filter(obj => checkedItems.contains(obj.id)).map(_.asInstanceOf[Thing])
        vars = varsToBe.map(_.asInstanceOf[Thing])
        SPIDs(sopSpecToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetSpecs(model = modelInfo.model))
        sopSpecs = sopSpecToBe.filter(obj => checkedItems.contains(obj.id) && obj.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec])

        //Create Supremica Module and synthesize guards.
        ptmw = ParseToModuleWrapper(modelInfo.name, vars, ops, sopSpecs)
        optSupervisorGuards = {
          ptmw.addVariables()
          ptmw.saveToWMODFile("./testFiles/gitIgnore/")
          ptmw.addOperations()
          ptmw.saveToWMODFile("./testFiles/gitIgnore/")
          ptmw.addForbiddenExpressions()
          ptmw.saveToWMODFile("./testFiles/gitIgnore/")
          ptmw.SupervisorAsBDD().getSupervisorGuards.map(_.filter(og => !og._2.equals("1")))
        }

        //Update operations with conditions and change to Supremica syntax
        updatedOps = ops.map(o => ptmw.addSPConditionFromAttributes(ptmw.addSynthesizedGuardsToAttributes(o, optSupervisorGuards), optSupervisorGuards))
        _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id, modelVersion = modelInfo.version, items = updatedOps))

      } yield {

          optSupervisorGuards.getOrElse(Map()).foreach(kv => println(s"${kv._1}: ${kv._2}"))
          //          updatedOps.foreach(o => println(s"${o.name} c:${o.conditions} a:${o.attributes.pretty}"))

          ptmw.addSupervisorGuardsToFreshFlower(optSupervisorGuards)
          ptmw.saveToWMODFile("./testFiles/gitIgnore/")

        }

      sender ! result
  }

}

object SynthesizeModelBasedOnAttributesService {
  def props(modelHandler: ActorRef) = Props(classOf[SynthesizeModelBasedOnAttributesService], modelHandler)
}

case class ParseToModuleWrapper(moduleName: String, vars: List[Thing], ops: List[Operation], sopSpec: List[SOPSpec]) extends FlowerPopulater with Exporters with Algorithms with TextFilePrefix {

  lazy val variableNameDomainMap = vars.flatMap(v => {
    val optDomain = v.attributes.findAs[Seq[String]]("domain").headOption
    if (optDomain.isDefined) Some(v.name -> optDomain.get) else None
  }).toMap

  lazy val mModule = SimpleModuleFactory(moduleName)

  private def directAttrValues(o: Operation, directAttr: Set[String]) = directAttr.flatMap(attr => o.attributes.getAs[Set[String]](attr)).flatten

  private def nestedAttrValues(o: Operation, nestedAttr: Set[TransformationPatternInAttributes => Option[String]], operator: String) = {
    Set("carrierTrans", "resourceTrans").flatMap { key =>
      o.attributes.getAs[SPAttributes](key).map {
        _.obj.flatMap { case (variable, toTpia) =>
          lazy val tpia = toTpia.to[TransformationPatternInAttributes].get
          nestedAttr.flatMap(_(tpia)).map(value => s"$variable $operator $value")
        }
      }
    }.flatten
  }

  private def addTransition(o: Operation, event: String, directGuardAttr: Set[String], nestedGuardAttr: Set[TransformationPatternInAttributes => Option[String]],
                            directActionAttr: Set[String], nestedActionAttr: Set[TransformationPatternInAttributes => Option[String]]) = {

    val allGuards = directAttrValues(o, directGuardAttr) ++ nestedAttrValues(o, nestedGuardAttr, "==")
    val guardAsString = if (allGuards.isEmpty) "" else allGuards.mkString("(", ")&(", ")")

    val actionsAsStrings = directAttrValues(o, directActionAttr) ++ nestedAttrValues(o, nestedActionAttr, "=")

    addLeaf(event, stringPredicateToSupremicaSyntax(guardAsString),
      actionsAsStrings.map(stringActionToSupremicaSyntax).mkString("; "))
  }

  def addOperations() = {
    ops.foreach { o =>
      //pre
      val startEvent = o.name
      addEventIfNeededElseReturnExistingEvent(startEvent, unControllable = false)
      addTransition(o, startEvent, Set("preGuard"), Set(_.atStart), Set("preAction"), Set(_.atExecute))

      //post
      val compEvent = s"$UNCONTROLLABLE_PREFIX$startEvent"
      addEventIfNeededElseReturnExistingEvent(compEvent, unControllable = true)
      addTransition(o, compEvent, Set("postGuard"), Set(_.atExecute), Set("postAction"), Set(_.atComplete))

      //Add operation events  to module comment
      mModule.setComment(s"$getComment$OPERATION_PREFIX${o.name} $TRANSITION_PREFIX$startEvent,$compEvent")
    }
  }

  def addVariables() = {
    vars.foreach { v => for {
      domain <- variableNameDomainMap.get(v.name)
      allInit = Set("init", "idleValue").flatMap(key => v.attributes.findAs[String](key))
      init <- if (allInit.size == 1) Some(allInit.head)
      else {
        println(s"Problem with variable ${v.name}, attribute keys init and idleValue do not point to the same value");
        None
      }
      intInit <- getFromVariableDomain(v.name, init, "Problem with init")
      idleValueAttr = v.attributes.getAs[String]("idleValue").map(Set(_))
      markingsAttr = v.attributes.getAs[Set[String]]("markings")
      allMarkings = Set(idleValueAttr, markingsAttr).flatten
      markings <- if (allMarkings.size == 1) Some(allMarkings.head)
      else {
        println(s"Problem with variable ${v.name}, attribute keys markings and idleValue do not point to the same value(s)");
        None
      }
      intMarkings = markings.flatMap(m => getFromVariableDomain(v.name, m, "Problem with marking"))
    } yield {
        addVariable(v.name, 0, domain.size - 1, intInit, intMarkings)
        //Add variable values to module comment
        mModule.setComment(s"$getComment${TextFilePrefix.VARIABLE_PREFIX}${v.name} d${TextFilePrefix.COLON}${domain.mkString(",")}")
      }
    }
  }

  def addForbiddenExpressions() = {
    lazy val operationIdMap = ops.map(o => o.id -> o).toMap
    sopSpec.foreach { s =>
      s.attributes.getAs[Set[String]]("forbiddenExpressions").foreach(fes =>
        addForbiddenExpression(forbiddenExpression = stringPredicateToSupremicaSyntax(fes.mkString("(", ")|(", ")")), addSelfLoop = false, addInComment = true))
      s.sop.foreach {
        case a: Arbitrary if a.sop.forall(_.isInstanceOf[Hierarchy]) =>
          lazy val opsExecuteMap = a.sop.map(_.asInstanceOf[Hierarchy]).flatMap(h => operationIdMap.get(h.operation)).map(o =>
            o -> (directAttrValues(o, Set("postGuard")) ++ nestedAttrValues(o, Set(_.atExecute), "==")).mkString("(", ")&(", ")")).toMap
          def addForbiddenExpressionForRemainingOps(remainingOps: Seq[Operation]): Unit = remainingOps match {
            case o +: os if os.nonEmpty =>
              os.foreach { otherO =>
                addForbiddenExpression(forbiddenExpression = stringPredicateToSupremicaSyntax(s"(${opsExecuteMap(o)})&(${opsExecuteMap(otherO)})"), addSelfLoop = false, addInComment = true)
              }
              addForbiddenExpressionForRemainingOps(os)
            case _ => //do nothing
          }
          addForbiddenExpressionForRemainingOps(opsExecuteMap.keys.toSeq)
        case _ => //do nothing
      }
    }
  }

  private def getFromVariableDomain(variable: String, value: String, errorMsg: String): Option[Int] = {
    variableNameDomainMap.get(variable) match {
      case Some(domain) => domain.indexOf(value) match {
        case -1 => println(s"$errorMsg\nValue: $value is not in the domain of variable: $variable. The result will not be correct!"); None
        case other => Some(other)
      }
      case _ => println(s"$errorMsg\nVariable: $variable is not defined. The result will not be correct!"); None

    }
  }

  def addSynthesizedGuardsToAttributes(o: Operation, optSynthesizedGuardMap: Option[Map[String, String]]) = {
    if (optSynthesizedGuardMap.isEmpty) o
    else {
      lazy val updatedAttribute = optSynthesizedGuardMap.get.get(o.name) match {
        case Some(guard) => o.attributes merge SPAttributes("preGuard" -> Set(guard))
        case _ => o.attributes
      }
      o.copy(attributes = updatedAttribute)
    }
  }

  def addSPConditionFromAttributes(o: Operation, optSynthesizedGuardMap: Option[Map[String, String]]): Operation = {
    if (optSynthesizedGuardMap.isEmpty) o
    else {
      def parseAttributesToPropositionCondition(op: Operation, idablesToParseFromString: List[IDAble]): Option[Operation] = {
        def getGuard(directGuardAttr: Set[String], nestedGuardAttr: Set[TransformationPatternInAttributes => Option[String]]) = {
          lazy val allGuards = directAttrValues(o, directGuardAttr) ++ nestedAttrValues(o, nestedGuardAttr, "==")
          lazy val guardAsString = if (allGuards.isEmpty) "" else allGuards.mkString("(", ")&(", ")")
          PropositionParser(idablesToParseFromString).parseStr(guardAsString) match {
            case Right(p) => Some(p)
            case Left(f) => println(s"PropositionParser failed for operation ${op.name} on guard: $guardAsString. Failure message: $f"); None
          }
        }
        def getAction(directActionAttr: Set[String], nestedActionAttr: Set[TransformationPatternInAttributes => Option[String]]) = {
          val actionsAsStrings = directAttrValues(o, directActionAttr) ++ nestedAttrValues(o, nestedActionAttr, "=")
          actionsAsStrings.flatMap { action =>
            ActionParser(idablesToParseFromString).parseStr(action) match {
              case Right(a) => Some(a)
              case Left(f) => println(s"ActionParser failed for operation ${op.name} on action: $action. Failure message: $f"); None
            }
          }.toList
        }

        for {
          preGuard <- getGuard(Set("preGuard"), Set(_.atStart))
          postGuard <- getGuard(Set("postGuard"), Set(_.atExecute))
        } yield {
          op.copy(conditions = List(PropositionCondition(preGuard, getAction(Set("preAction"), Set(_.atExecute)), SPAttributes("kind" -> "precondition")),
            PropositionCondition(postGuard, getAction(Set("postAction"), Set(_.atComplete)), SPAttributes("kind" -> "postcondition"))))
        }
      }
      parseAttributesToPropositionCondition(o, vars).getOrElse(o)
    }
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