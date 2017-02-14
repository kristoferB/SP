package sp.supremicaStuff.base

import scala.collection.JavaConverters._
import net.sourceforge.waters.subject.module.BinaryExpressionSubject
import net.sourceforge.waters.subject.module.SimpleIdentifierSubject
import net.sourceforge.waters.subject.module.IntConstantSubject
import net.sourceforge.waters.subject.module.EdgeSubject
import net.sourceforge.waters.model.module.EdgeProxy
import net.sourceforge.waters.model.module.SimpleExpressionProxy
import net.sourceforge.waters.model.expr.BinaryOperator
import net.sourceforge.waters.model.module.{ BinaryExpressionProxy, VariableComponentProxy, VariableMarkingProxy }

trait StateTransExplorer extends Workers {
  type State = Map[String, Int]

  def getTextForVariableValuesFromModuleComments() = {
    getComment.split("\n").map(str => parseAll(s"${TextFilePrefix.VARIABLE_PREFIX}".r ~> s"(.(?!:))+".r ~ (s"d${TextFilePrefix.COLON}".r ~> s".*".r), str.replaceAll(" ", "")) match {
      case Success(~(name, data), _) => Some(name -> data.split(",").toSeq)
      case _ => None
    }).flatten.toMap
  }

  //Looks in module comment to see if there exists a text value for the integer value used in the module for each variable.
  protected def unmaskState(state: State, umMaskMap: Map[String, Seq[String]] = getTextForVariableValuesFromModuleComments()): Map[String, String] = state.map {
    case (name, value) => name -> (umMaskMap.get(name) match {
      case Some(domain) if domain.length > value && value >= 0 => domain(value)
      case _ => value.toString
    })
  }

  private lazy val besVar = getVariables.map(_.getInitialStatePredicate().asInstanceOf[BinaryExpressionSubject])
  protected def initState() = besVar.map(pred => pred.getLeft().asInstanceOf[SimpleIdentifierSubject].getName() -> pred.getRight().asInstanceOf[IntConstantSubject].getValue()).toMap

  def evalMarking(v: VariableComponentProxy)(state: State) = {
    def evalMarkingInner(markings: Seq[VariableMarkingProxy] = v.getVariableMarkings.asScala, toReturn: Boolean = false): Boolean = markings match {
      case m +: ms => evalExpression(m.getPredicate.asInstanceOf[BinaryExpressionProxy])(state) match {
        case Some(true) => evalMarkingInner(ms, true)
        case _ => false
      }
      case _ => toReturn //Thus if no markings in list initValue for toReturn (=false) is returned
    }
    evalMarkingInner()
  }
  lazy val varMarkMap = getVariables.map { v => v.getName() -> evalMarking(v)_ }.toMap

  protected def isStateMarked(state: State): Boolean = state forall {case s@(key,_) => varMarkMap(key)(Map(s))}

  case class GuardAction(guard: Option[SimpleExpressionProxy] = None, action: Option[Iterable[BinaryExpressionProxy]] = None) {
    def combine(that: GuardAction) = {
      val newGuard = if (guard.isDefined & that.guard.isDefined) parseGuardStr(s"(${guard.get.toString}) & (${that.guard.get.toString})")
      else if (guard.isDefined) guard else that.guard
      val newAction = if (action.isDefined & that.action.isDefined) {
        def bepMap(actionIt: Iterable[BinaryExpressionProxy]) = actionIt.map(bep => bep.getLeft().toString() -> bep).toMap
        Some(bepMap(action.get).foldLeft(bepMap(that.action.get)) {
          case (acc, (variable, bep)) =>
            if (acc.contains(variable)) {
              if (!bep.toString.equals(acc(variable).toString)) { throw new UnsupportedOperationException(s"The variable: $variable is updated to different values with different actions, I will not go on!") }
            }
            acc + (variable -> bep)
        }.values)
      } else if (action.isDefined) action else that.action
      GuardAction(newGuard, newAction)
    }
  }

  protected def getTsEnabledFromSpecificState(state: State): Map[String, State => State] = {
    val enabledTrans = getTs.filter { case (_, ga) => ga.guard.isDefined }.filter { case (_, ga) => evalExpression(ga.guard.get)(state).getOrElse(false) }
    enabledTrans.filter { case (_, ga) => ga.action.isDefined }.map { case (event, ga) => event -> updateState(ga.action.get)_ }
  }

  protected lazy val getTs: Map[String, GuardAction] = {
    val allEdges = getFlowers.flatMap(_.getGraph().getEdges().asScala.filter(_.isInstanceOf[EdgeSubject]))
    allEdges.foldLeft(Map(): Map[String, GuardAction]) {
      case (acc0, edge) =>
        edge.getLabelBlock().getEventIdentifierList().asScala.foldLeft(acc0) {
          case (acc1, proxy) =>
            val event = proxy.toString
            val guardAction = GuardAction(getGuard(edge), getAction(edge))
            acc1 + (if (acc1.contains(event)) event -> acc1(event).combine(guardAction) else event -> guardAction)
        }
    }
  }

  protected def getGuard(edge: EdgeProxy): Option[SimpleExpressionProxy] = {
    if (edge.getGuardActionBlock() == null) None else {
      val guardSeq = edge.getGuardActionBlock().getGuards()
      if (guardSeq.isEmpty()) None else Some(guardSeq.get(0))
    }
  }

  protected def getAction(edge: EdgeProxy): Option[Iterable[BinaryExpressionProxy]] = {
    if (edge.getGuardActionBlock() == null) None else {
      val actionSeq = edge.getGuardActionBlock().getActions()
      if (actionSeq.isEmpty()) None else Some(actionSeq.asScala)
    }
  }

  private def updateState(actionIt: Iterable[BinaryExpressionProxy])(state: State): State = {

    var localState = state

    val it = actionIt.iterator

    while (it.hasNext) {
      val action = it.next()

      lazy val operator = action.getOperator()

      def rightSide: Int = {
        if (action.getRight().isInstanceOf[IntConstantSubject]) {
          action.getRight().asInstanceOf[IntConstantSubject].getValue()
        } else if (action.getRight().isInstanceOf[SimpleIdentifierSubject]) {
          val variable = action.getRight().asInstanceOf[SimpleIdentifierSubject].getName()
          state.get(variable).get
        } else throw new IllegalArgumentException("Type of action not known!")
      }
      if (action.getLeft().isInstanceOf[SimpleIdentifierSubject] && (
        operator.equals(mOptable.getAssignmentOperator()) ||
        operator.equals(mOptable.getIncrementOperator()) ||
        operator.equals(mOptable.getDecrementOperator()))) {
        localState = updateState(action.getLeft().asInstanceOf[SimpleIdentifierSubject].getName(),
          operator, rightSide)
      } else throw new IllegalArgumentException("Type of action not known!")
    }

    def updateState(variableName: String, operator: BinaryOperator, value: Int): State = {
      if (!localState.contains(variableName)) throw new IllegalArgumentException("Variable unknown to state")
      else if (operator.equals(mOptable.getAssignmentOperator())) localState.updated(variableName, value)
      else if (operator.equals(mOptable.getIncrementOperator())) localState.updated(variableName, localState.get(variableName).get + value)
      else localState.updated(variableName, localState.get(variableName).get - value) //operator.equals(cot.getDecrementOperator())
    }
    localState
  }

  //state1 = Map("a" -> 1, "b" -> 2, "c" -> 3)
  //state2 = Map("a" -> 1, "b" -> 1)
  //return = Set("a" -> 1)
  def getIntersectionSubState(state1: State, state2: State): State = state1.foldLeft(Map(): State) {
    case (acc, (k1, v1)) => state2.get(k1) match {
      case Some(v2) => if (v1.equals(v2)) acc + (k1 -> v1) else acc
      case _ => acc
    }
  }
}