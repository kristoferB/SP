package sp.supremicaStuff.base

import net.sourceforge.waters.subject.module.ModuleSubject
import java.io.File
import net.sourceforge.waters.model.marshaller.JAXBModuleMarshaller
import net.sourceforge.waters.subject.module.ModuleSubjectFactory
import net.sourceforge.waters.model.compiler.CompilerOperatorTable
import net.sourceforge.waters.model.module.EventDeclProxy
import net.sourceforge.waters.xsd.base.EventKind
import scala.io.Source
import scala.util.parsing.combinator.RegexParsers

trait SimpleModuleFactory {
  def moduleFactory(iModuleName: String, iModuleComment: Option[String] = None) = {
    val ms = new ModuleSubject(iModuleName, null)
    iModuleComment match {
      case Some(comment) => ms.setComment(comment)
      case _ =>
    }
    ms
  }
  def initModule(ms: ModuleSubject) = { //Add marking to module
    ms.getEventDeclListModifiable().add(new ModuleSubjectFactory().createEventDeclProxy(new ModuleSubjectFactory().createSimpleIdentifierProxy(EventDeclProxy.DEFAULT_MARKING_NAME), EventKind.PROPOSITION)); ms
  }
}

object SimpleModuleFactory extends SimpleModuleFactory {
  def apply(iModuleName: String, iModuleComment: Option[String] = None): ModuleSubject = initModule(moduleFactory(iModuleName, iModuleComment))
}

object ReadFromWmodFileModuleFactory {
  def apply(iFilePath: String): Option[ModuleSubject] = {
    try {
      val fileUri = (new File(iFilePath)).toURI()
      val marshaller = new JAXBModuleMarshaller(new ModuleSubjectFactory(), CompilerOperatorTable.getInstance())
      return Some(marshaller.unmarshal(fileUri).asInstanceOf[ModuleSubject])
    } catch { case t: Throwable => println(t) }
    None
  }
}

object TextFilePrefix extends TextFilePrefix
trait TextFilePrefix {
  lazy val COLON = ":"
  lazy val NAME = "\\w+"
  lazy val NAMES = s"\\{?((\\s*$NAME,)*\\s*$NAME\\s*)\\}?"
  lazy val VARIABLE_PREFIX = "v" + COLON
  lazy val TRANSITION_PREFIX = "t" + COLON
  lazy val TRANSITION_SET_PREFIX = "T" + COLON
  lazy val FORBIDDEN_PREFIX = "x" + COLON
  lazy val OPERATION_PREFIX = "o" + COLON
  lazy val COMMENT = "//"
  lazy val UNCONTROLLABLE_PREFIX = "uc_"
  lazy val ALL = "(All-)"
}

trait ParseTextFileToModule extends SimpleModuleFactory with FlowerPopulater with RegexParsers with TextFilePrefix {
  var mValueStringIntMap: Map[String, String] = Map()
  def parseTextFileToModule(iTextFilePath: String): Boolean = {
    val lines = Source.fromFile(s"$iTextFilePath").getLines().toSeq

    //Variables-------------------------------------------------------------------------------------
    case class ParsedVariable(name: String, data: Option[Map[String, String]], comment: Option[String])
    lazy val dataTypes = Map("domain" -> "d", "init" -> "i", "marked" -> "m")
    def parseVariable(data: String) = {
      val updatedData = dataTypes.foldLeft(data.replaceAll(" |\t", "")) { case (updData, (full, short)) => updData.replaceAll(s"$full$COLON", s"$short$COLON") }
      val typeRegex = s"${dataTypes.values.mkString("(", "|", ")")}".r <~ s":".r
      val valueRegex = s"(.(?!:))+".r //As an example "named:" => "name"
      parseAll((typeRegex ~ valueRegex)*, updatedData) match {
        case Success(list, _) => Some(list.map { case (~(name, data)) => name -> data }.toMap)
        case _ => None
      }
    }
    lazy val variables = lines.flatMap(str => parseAll((s"$VARIABLE_PREFIX".r ~> s"$NAME".r) ~ (s"[^/]+".r) ~ opt(s"$COMMENT".r ~> s".*".r), str) match {
      case Success(~(~(name, data), comment), _) => Some(ParsedVariable(name, parseVariable(data), comment))
      case _ => None
    })

    variables.foreach(v => {
      if (v.data.getOrElse(Map()).size != 3) { println(s"Problem with variable ${v.name}! One domain value, one init value, and one marked value are required!"); return false }
      val rangeDomain = v.data.get("d").split("\\.\\.")
      if (rangeDomain.size == 2) { //must be a variables with integers 
        val markedValues = v.data.get("m").split(",").map(Integer.parseInt).toSet
        if (!addVariable(v.name, Integer.parseInt(rangeDomain(0)), Integer.parseInt(rangeDomain(1)), Integer.parseInt(v.data.get("i")), markedValues).isDefined) { return false }
      } else { //must be a variables with values 
        val domain = v.data.get("d").split(",")
        if (!valuesFromStringToIntRepresentationFactory(domain)) { return false }
        val markedValues = v.data.get("m").split(",").map(mv => domain.indexOf(mv)).toSet
        if (!addVariable(v.name, 0, domain.size - 1, domain.indexOf(v.data.get("i")), markedValues).isDefined) { return false }
      }
      //Add variable values to module comment
      mModule.setComment(s"$getComment${TextFilePrefix.VARIABLE_PREFIX}${v.name} d${TextFilePrefix.COLON}${v.data.get("d")}")
      if(v.comment.isDefined) mModule.setComment(s"$getComment${v.comment.get}")
    })

    //Transitions--------------------------------------------------------------------------------------
    case class ParsedTrans(name: String, data: (String, String), comment: Option[String]) { def guard = data._1; def action = data._2 }
    def parseGuardAction(data: String) = {
      data.replaceAll(("\\s|\\t"), "").split(s"c$COLON").foldLeft(("", "")) {
        case ((guard, action), condition) =>
          var localGuard = guard; var localAction = action
          if (!condition.isEmpty()) {
            val index = condition.indexOf(";")
            if (index != 0) {
              if (!guard.isEmpty()) { localGuard += "&" }
              if (index < 0) { localGuard += "(" + condition + ")" } else { localGuard += "(" + condition.substring(0, index) + ")" } // c:guard else c:guard;action(;action)*
            }
            if (index >= 0) { localAction += condition.substring(index) } // c:;action(;action)* || c:guard;action(;action)*
          }
          (localGuard, localAction)
      }
    }
    lazy val trans = lines.map(str => parseAll(s"$TRANSITION_PREFIX".r ~> s"$NAME".r ~ s"[^/]+".r ~ opt(s"$COMMENT".r ~> s".*".r), str) match {
      case Success(~(~(name, data), comment), _) => Some(ParsedTrans(name, parseGuardAction(data), comment))
      case _ => None
    }).flatten

    trans.foreach {
      t => addLeafAndEventToAlphabet(t.name, t.name.startsWith(UNCONTROLLABLE_PREFIX), valuesFromStringToIntRepresentation(t.guard), valuesFromStringToIntRepresentation(t.action))
      if(t.comment.isDefined) mModule.setComment(s"$getComment${t.comment.get}")
    }

    //Transition sets-------------------------------------------------------------
    case class ParsedTransSet(names: Option[Seq[String]], data: (String, String), comment: Option[String]) { def guard = data._1; def action = data._2 }
    def parseTransSet(all: Option[String], names: String): Option[Seq[String]] = {
      val transFromNames = names.replaceAll(" ", "").split(",")
      transFromNames.filterNot(getAlphabet.map(_.getName()).contains).foreach { t => println(s"The transition: $t has not been defined."); return None } // Are all transitions defined?
      Some(if (all.isDefined) getAlphabet.map(_.getName()).filterNot(transFromNames.contains).toSeq else transFromNames) // Work with given transitions or complement
    }
    lazy val transSet = lines.map(str => parseAll(s"$TRANSITION_SET_PREFIX".r ~> opt(s"$ALL".r) ~ s"$NAMES".r ~ s"[^/]+".r ~ opt(s"$COMMENT".r ~> s".*".r), str) match {
      case Success(~(~(~(all, names), data), comment), _) => Some(ParsedTransSet(parseTransSet(all, names.replaceAll("\\{|\\}", "")), parseGuardAction(data), comment))
      case _ => None
    }).flatten

    transSet.foreach { ts =>
      if (!ts.names.isDefined) { println("Problem to parse set of transitions"); return false }
      ts.names.get.foreach(t => addLeaf(t, valuesFromStringToIntRepresentation(ts.guard), valuesFromStringToIntRepresentation(ts.action)))
    }

    //Forbidden state combinations------------------------------------------------
    lazy val fscs = lines.map(str => parseAll(s"$FORBIDDEN_PREFIX".r ~> s"$NAME".r ~ s"[^/]+".r ~ opt(s"$COMMENT".r ~> s".*".r), str) match {
      case Success(~(~(name, data), comment), _) => Some(data)
      case _ => None
    }).flatten

//    fscs.foreach(d => addForbiddenExpression(valuesFromStringToIntRepresentation(d), addSelfLoop = false, addInComment = true))
        fscs.foreach(d => addForbiddenExpression(valuesFromStringToIntRepresentation(d), true, false))

    //Parse was ok
    return true
  }

  private def valuesFromStringToIntRepresentationFactory(valueDomain: Seq[String]): Boolean = {
    valueDomain.filter(mValueStringIntMap.contains).filter(v => !mValueStringIntMap(v).equals(Integer.toString(valueDomain.indexOf(v)))).foreach { v =>
      println(s"The value $v appears at two different positions in two different domains. For parsing simplicity, this is not allowed!"); return false
    }
    valueDomain.filter(v => getVariables.map(_.getName()).contains(v)).foreach { v =>
      println(s"The value $v coincides with the name of a variable. For parsing simplicity, this is not allowed!"); return false
    }
    mValueStringIntMap = valueDomain.foldLeft(mValueStringIntMap) { case (acc, v) => acc + (v -> Integer.toString(valueDomain.indexOf(v))) }
    return true
  }
  private def valuesFromStringToIntRepresentation(expr: String): String = mValueStringIntMap.foldLeft(expr) { case (acc, (k, v)) => acc.replaceAll(s"\\b$k\\b", v) }

}