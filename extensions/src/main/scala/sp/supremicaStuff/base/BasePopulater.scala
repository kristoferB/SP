package sp.supremicaStuff.base

import net.sourceforge.waters.model.module.SimpleExpressionProxy
import net.sourceforge.waters.subject.module.ModuleSubject
import net.sourceforge.waters.subject.module.ModuleSubjectFactory
import net.sourceforge.waters.model.compiler.CompilerOperatorTable
import net.sourceforge.waters.model.expr.ExpressionParser
import net.sourceforge.waters.subject.module.SimpleExpressionSubject
import net.sourceforge.waters.model.expr.Operator
import net.sourceforge.waters.model.module.EventDeclProxy
import java.util.Collections
import net.sourceforge.waters.subject.module.VariableComponentSubject
import java.util.ArrayList
import net.sourceforge.waters.model.module.SimpleIdentifierProxy
import scala.collection.JavaConverters._
import net.sourceforge.waters.subject.module.SimpleComponentSubject
import net.sourceforge.waters.xsd.base.ComponentKind
import net.sourceforge.waters.subject.module.GraphSubject
import java.util.LinkedList
import net.sourceforge.waters.xsd.base.EventKind
import net.sourceforge.waters.model.module.NodeProxy
import net.sourceforge.waters.subject.module.BinaryExpressionSubject
import net.sourceforge.waters.subject.module.EdgeSubject
import net.sourceforge.waters.model.expr.TypeMismatchException
import net.sourceforge.waters.subject.module.NodeSubject

/**
 * Add variables
 * Add events
 * Add roots (Single location efa)
 */
trait BasePopulater extends Workers {

  private def addVariable(name: String, typeOfVar: SimpleExpressionProxy, initValue: SimpleExpressionProxy, markedValues: Set[String]): Option[VariableComponentSubject] = {
    val markedPredicate = if (markedValues.isEmpty) None else Some(markedValues.map(name + "==" + _).mkString("|"))
    var markedPred: SimpleExpressionSubject = null
    try {
      if (markedPredicate.isDefined) { markedPred = mParser.parse(markedPredicate.get, Operator.TYPE_BOOLEAN).asInstanceOf[SimpleExpressionSubject] }
    } catch {
      case _: Throwable => println("Problem when parsing marking:\n Variable: " + name + "\n Expression: " + markedPredicate); return None
    }
    
    val initPredicate = mFactory.createBinaryExpressionProxy(mOptable.getEqualsOperator(), mFactory.createSimpleIdentifierProxy(name), initValue)
    val accepting = mFactory.createSimpleIdentifierProxy(EventDeclProxy.DEFAULT_MARKING_NAME)
    val marking = mFactory.createVariableMarkingProxy(accepting, markedPred)
    val markings = Collections.singletonList(marking)
    val vcs = new VariableComponentSubject(mFactory.createSimpleIdentifierProxy(name), typeOfVar, initPredicate, markings)

    mModule.getComponentListModifiable().add(vcs)

    Some(vcs)
  }

  def addVariable(name: String, lowerBound: Int, upperBound: Int, initialValue: Int, markedValues: Set[Int]): Option[VariableComponentSubject] = {
    val lower = mFactory.createIntConstantProxy(lowerBound)
    val upper = mFactory.createIntConstantProxy(upperBound)
    val oprange = mOptable.getRangeOperator()
    val range = mFactory.createBinaryExpressionProxy(oprange, lower, upper)

    if (initialValue < lowerBound || initialValue > upperBound) { println("Problem when creating variable, initial value not in domain:\n Variable: " + name); return None }
    markedValues.foreach(value => if (value < lowerBound || value > upperBound) { println("Problem when creating variable, a marked value not in domain:\n Variable: " + name); return None })

    addVariable(name, range, mFactory.createIntConstantProxy(initialValue), markedValues.map(Integer.toString(_)))
  }

  def addVariable(name: String, domainValues: Set[String], initialValue: String, markedValues: Set[String]): Option[VariableComponentSubject] = {
    if (!domainValues.contains(initialValue)) { println("Problem when creating variable, initial value not in domain:\n Variable: " + name); return None }
    if (!markedValues.filter(!domainValues.contains(_)).isEmpty) { println("Problem when creating variable, marked values not in domain:\n Variable: " + name); return None }
    val domain = domainValues.map(mFactory.createSimpleIdentifierProxy(_)).asJava

    addVariable(name, mFactory.createEnumSetExpressionProxy(domain), mFactory.createSimpleIdentifierProxy(initialValue), markedValues)
  }

  def addFlowerRoot(name: String): Option[SimpleComponentSubject] = {
    if (getEFAs.map(_.getName()).contains(name)) { return None }

    val ident = mFactory.createSimpleIdentifierProxy(name)
    val graph = mFactory.createGraphProxy()
    val scs = new SimpleComponentSubject(ident, ComponentKind.PLANT, graph)

    mModule.getComponentListModifiable().add(scs)
    addLocation(graph, "pm") match {
      case None => return None
      case _ => Some(scs)
    }
  }

  private def addLocation(graph: GraphSubject, name: String, isInitial: Boolean = true, isMarked: Boolean = true): Option[NodeProxy] = {
    if (graph.getNodesModifiable().asScala.map(_.getName()).contains(name)) { return None }
    val propList = Seq(mFactory.createSimpleIdentifierProxy(EventDeclProxy.DEFAULT_MARKING_NAME))
    val node = mFactory.createSimpleNodeProxy(name, mFactory.createPlainEventListProxy(propList.asJava), null, isInitial, null, null, null)
    graph.getNodesModifiable().add(node)
    Some(node)
  }

  def addEventIfNeededElseReturnExistingEvent(eventName: String, unControllable: Boolean = false): Some[EventDeclProxy] = {
    val result = getAlphabet.filter(_.getName().equals(eventName))
    if (!result.isEmpty) Some(result.head) else {
      val ident = mFactory.createSimpleIdentifierProxy(eventName)
      val edp = mFactory.createEventDeclProxy(ident, if (unControllable) EventKind.UNCONTROLLABLE else EventKind.CONTROLLABLE)
      mModule.getEventDeclListModifiable().add(edp)
      Some(edp)
    }
  }

  /*
   * Not that the event must be added to the module alphabet separately, if needed
   */
  private def addEdge(graph: GraphSubject, eventName: String, fromNode: NodeProxy, toNode: NodeProxy, guard: Option[SimpleExpressionSubject], actions: Option[Set[BinaryExpressionSubject]]): Option[EdgeSubject] = {
    val labelBlock = mFactory.createLabelBlockProxy(Seq(mFactory.createSimpleIdentifierProxy(eventName)).asJava, null);
    val guardActionBlock = mFactory.createGuardActionBlockProxy()
    if (guard.isDefined) { guardActionBlock.getGuardsModifiable().add(guard.get) }
    if (actions.isDefined) { actions.get.foreach(guardActionBlock.getActionsModifiable().add(_)) }
    val edge = mFactory.createEdgeProxy(fromNode, toNode, labelBlock, guardActionBlock, null, null, null);
    graph.getEdgesModifiable().add(edge)
    Some(edge)
  }

  def addEdge(graph: GraphSubject, eventName: String, fromNode: NodeProxy, toNode: NodeProxy, guardAsText: String, actionsAsText: String): Option[EdgeSubject] = {
    val guard = parseGuardStr(guardAsText)
    val actions = parseActionsStr(actionsAsText)
    addEdge(graph, eventName, fromNode, toNode, guard, actions)
  }

}