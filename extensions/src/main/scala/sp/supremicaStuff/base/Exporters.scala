package sp.supremicaStuff.base

import net.sourceforge.waters.subject.module.ModuleSubject
import scala.collection.JavaConverters._
import java.io.File
import net.sourceforge.waters.model.marshaller.JAXBModuleMarshaller
import java.util.Calendar
import net.sourceforge.waters.model.module.VariableComponentProxy
import net.sourceforge.waters.model.module.BinaryExpressionProxy
import scala.util.parsing.combinator.RegexParsers
import sp.supremicaStuff.auxiliary.MySupport


trait Exporters extends BaseFunctionality with RegexParsers with StateTransExplorer {
  def saveToWMODFile(iFilePath: String, iModule: ModuleSubject = mModule): Boolean = {
    try {
      val file = new File(iFilePath + (if (!iFilePath.endsWith(".wmod")) iModule.getName() + ".wmod" else ""))
      val marshaller = new JAXBModuleMarshaller(mFactory, mOptable)
      iModule.setComment((if (getComment != null) getComment + "\n" else "") + "File generated: " + Calendar.getInstance().getTime())
      marshaller.marshal(iModule, file)
      return true
    } catch {
      case t: Throwable => println(t)
    }
    false
  }

  def saveToJSON(iFilePath: String, iModule: ModuleSubject = mModule): Boolean = {
    import org.json4s.JsonDSL._
    import org.json4s.native.JsonMethods._

    //VARIABLES------------------------------------------------------------------------------------------------
    def variableToJSON(v: VariableComponentProxy) = {
      val name = v.getName
      val domain = v.getType.asInstanceOf[BinaryExpressionProxy]

      ("isa" -> "Thing") ~
        ("name" -> name) ~
        ("attributes" ->
          ("low" -> Integer.parseInt(s"${domain.getLeft}")) ~
          ("high" -> Integer.parseInt(s"${domain.getRight}")) ~
          ("markings" -> s"${v.getVariableMarkings.asScala.map(m => s"(${m.getPredicate})").mkString("&")}") ~
          ("stateVariable" ->
            ("domain" -> getTextForVariableValuesFromModuleComments.getOrElse(name, Seq())) ~
            ("init" -> Integer.parseInt(s"${v.getInitialStatePredicate.asInstanceOf[BinaryExpressionProxy].getRight}")) ~
            ("goal" -> {
              v.getVariableMarkings().asScala match {
                case ms if (ms.length == 1) => {
                  val bep = ms.head.getPredicate().asInstanceOf[BinaryExpressionProxy]
                  bep.getOperator().getName() match {
                    case "==" => Some(Integer.parseInt(bep.getRight().toString()))
                    case _ => (None: Option[Int])
                  }
                }
                case _ => (None: Option[Int])
              }
            })))
    }

    //OPERATIONS--------------------------------------------------------------------------------------------------
    val startTransions = getTs.filterKeys(!_.startsWith(TextFilePrefix.UNCONTROLLABLE_PREFIX))
    val compTransions = getTs.filterKeys(_.startsWith(TextFilePrefix.UNCONTROLLABLE_PREFIX))

    def operationToJSON(event: String) = {
      val pre = startTransions(event)
      val post = compTransions(s"${TextFilePrefix.UNCONTROLLABLE_PREFIX}$event")

      ("isa" -> "Operation") ~
        ("name" -> event) ~
        ("conditions" -> (Seq(): Seq[String])) ~
        ("attributes" ->
          ("preGuard" -> s"${pre.guard.get}") ~
          ("preAction" -> pre.action.get.map(_.toString)) ~
          ("postGuard" -> s"${post.guard.get}") ~
          ("postAction" -> post.action.get.map(_.toString)))
    }

    //PRINT TO FILE-------------------------------------------------------------------------------------------------
    MySupport.saveToFile(iFilePath, Seq(pretty(render(getVariables.map(v => variableToJSON(v)).toSeq ++ startTransions.keys.map(t => operationToJSON(t))))))
  }
}