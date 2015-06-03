package sp.supremicaStuff.base

import net.sourceforge.waters.subject.module.ModuleSubject
import net.sourceforge.waters.subject.module.ModuleSubjectFactory
import net.sourceforge.waters.model.compiler.CompilerOperatorTable
import net.sourceforge.waters.model.expr.ExpressionParser
import net.sourceforge.waters.model.module.EventDeclProxy
import net.sourceforge.waters.xsd.base.EventKind
import net.sourceforge.waters.model.module.VariableComponentProxy
import net.sourceforge.waters.subject.module.SimpleComponentSubject
import scala.collection.JavaConverters._
import net.sourceforge.waters.model.module.EnumSetExpressionProxy

trait BaseFunctionality {
  val mModule: ModuleSubject
  lazy val mFactory = new ModuleSubjectFactory()
  lazy val mOptable = CompilerOperatorTable.getInstance()
  lazy val mParser = new ExpressionParser(mFactory, mOptable)

  def getAlphabet = mModule.getEventDeclListModifiable().asScala.filter(!_.getName().equals(EventDeclProxy.DEFAULT_MARKING_NAME))
  def getVariables = mModule.getComponentListModifiable().asScala.filter(_.isInstanceOf[VariableComponentProxy]).map(_.asInstanceOf[VariableComponentProxy])
  def getEFAs = mModule.getComponentListModifiable().asScala.filter(_.isInstanceOf[SimpleComponentSubject]).map(_.asInstanceOf[SimpleComponentSubject])
  def getFlowers = getEFAs.filter(_.getGraph().getNodes().size() == 1)
  def getComment = if (mModule.getComment() == null) "" else s"${mModule.getComment()}\n"
}

object SupFlower extends App {
  def start(args : Array[String] = Array()) {
    println(s"SupFlower has started")
    println(s"${args.mkString("\n")}")
  }
}