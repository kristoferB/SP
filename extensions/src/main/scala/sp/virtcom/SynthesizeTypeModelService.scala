package sp.virtcom

import akka.actor._
import org.json4s.JsonAST.{JObject, JString}
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
 * Created by patrik on 2015-06-04.
 */
class SynthesizeTypeModelService(modelHandler: ActorRef) extends Actor with ServiceSupportTrait {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(service, attr) => {

      println(s"service: $service")

      val id = attr.getAs[ID]("activeModelID").getOrElse(ID.newID)

      val result = for {
        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))

        //Collect ops, vars, forbidden expressiones
        SPIDs(opsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetOperations(model = modelInfo.model))
        ops = opsToBe.map(_.asInstanceOf[Operation])
        SPIDs(varsToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetThings(model = modelInfo.model))
        vars = varsToBe.map(_.asInstanceOf[Thing])
        SPIDs(spSpecToBe) <- futureWithErrorSupport[SPIDs](modelHandler ? GetSpecs(model = modelInfo.model))
        spec = spSpecToBe.map(_.asInstanceOf[SPSpec])

        //Create Supremica Module and synthesize guards.
        ptmw = ParseToModuleWrapper(modelInfo.name, vars, ops, spec)
        supervisorGuards = {
          ptmw.addVariables()
          ptmw.addOperations()
          ptmw.addForbiddenExpressions()
          ptmw.saveToWMODFile("./testFiles/gitIgnore/")
          ptmw.getSupervisorGuardsAsSPAttributes()
        }

        updatedOps = ops.map(o => ptmw.createFinalCondition(o,supervisorGuards.get(o.name)))
        _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id, modelVersion = modelInfo.version, items = updatedOps))

      } yield {
          updatedOps.foreach(o => println(s"${o.name} c:${o.conditions} a:${o.attributes.pretty}"))
        }

      sender ! result

    }
  }

}

object SynthesizeTypeModelService {
  def props(modelHandler: ActorRef) = Props(classOf[SynthesizeTypeModelService], modelHandler)
}

case class ParseToModuleWrapper(moduleName: String, vars: List[Thing], ops: List[Operation], spec: List[SPSpec]) extends FlowerPopulater with Exporters with Algorithms with TextFilePrefix {

  lazy val variableMap = vars.flatMap(v => {
    val optDomain = v.attributes.findAs[Seq[String]]("domain").headOption
    if (optDomain.isDefined) Some(v.name -> optDomain.get) else None
  }).toMap

  lazy val mModule = SimpleModuleFactory(moduleName)

  private def addTransition(o: Operation, event: String, guard: String, action: String) = {
    val guardAsString = o.attributes.findAs[String](guard).mkString("(", ")&(", ")")
    val actionsAsString = o.attributes.findAs[String](action)
    addLeaf(event, stringPredicateToSupremicaSyntax(guardAsString),
      actionsAsString.map(stringActionToSupremicaSyntax).mkString("; "))
  }

  def addOperations() = {
    ops.foreach { o =>
      //pre
      val startEvent = o.name
      addEventIfNeededElseReturnExistingEvent(startEvent, false)
      addTransition(o, startEvent, "preGuard", "preAction")
      //post
      val compEvent = s"$UNCONTROLLABLE_PREFIX$startEvent"
      addEventIfNeededElseReturnExistingEvent(compEvent, true)
      addTransition(o, compEvent, "postGuard", "postAction")
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

  def getSupervisorGuardsAsSPAttributes() = {
    def stringToAttribute(s: String) = s match {
      case "1" => SPAttributes()
      case "0" => SPAttributes("preGuard" -> "1<0")
      case v => SPAttributes("preGuard" -> v)
    }
    getSupervisorGuards().getOrElse(Map()).map { case (o, v) => o -> stringToAttribute(v) }
  }

  def createFinalCondition(o: Operation, synthesizedGuard : Option[SPAttributes]): Operation = {
    val updatedAttribute = o.attributes.mapField {
      case ("preGuard", JString(v)) => ("preGuard", JString(stringPredicateToSupremicaSyntax(v)))
      case ("preAction", JString(v)) => ("preAction", JString(stringActionToSupremicaSyntax(v)))
      case ("postGuard", JString(v)) => ("postGuard", JString(stringPredicateToSupremicaSyntax(v)))
      case ("postAction", JString(v)) => ("postAction", JString(stringActionToSupremicaSyntax(v)))
      case other => other
    }

    val opWithUpdatedAttributes = o.copy(attributes = updatedAttribute.getAs[JObject].getOrElse(SPAttributes())+synthesizedGuard.getOrElse(SPAttributes()))
    PropositionConditionLogic.parseAttributesToPropositionCondition(opWithUpdatedAttributes,vars).getOrElse(opWithUpdatedAttributes)
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


  //To get correct syntax of guards in Supremica
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
    val optValue = a.value match {
      case ValueHolder(v) => v.getAs[String]
      case _ => None
    }
    for {
      variable <- varsIdMap.get(a.id)
      value <- optValue
      valueInt <- getFromVariableDomain(variable, value, "Problem with action")
    } yield {
      s"$variable = $valueInt"
    }
  }

  private def stringPredicateToSupremicaSyntax(s: String) = PropositionParser().parseStr(s) match {
    case Right(p) => propToSupremicaSyntax(p)
    case other => other.toString
  }

  private def leftRight(l: StateEvaluator, operator: String, r: StateEvaluator) = {
    val left = stateEvalToSupremicaSyntax(l)
    val right = stateEvalToSupremicaSyntax(r)
    val result = for {
      value <- getFromVariableDomain(left, right, "Problem with guard")
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