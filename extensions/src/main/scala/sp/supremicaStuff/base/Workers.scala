package sp.supremicaStuff.base

import net.sourceforge.waters.subject.module.SimpleExpressionSubject
import net.sourceforge.waters.model.expr.Operator
import net.sourceforge.waters.subject.module.BinaryExpressionSubject
import net.sourceforge.waters.model.expr.TypeMismatchException
import net.sourceforge.waters.model.module.SimpleExpressionProxy
import net.sourceforge.waters.model.compiler.context.ModuleBindingContext
import net.sourceforge.waters.model.compiler.context.SimpleExpressionCompiler

trait Workers extends Algorithms {

  def parseGuardStr[T](guardAsText: String): Option[T] = {
    try {
      if (guardAsText != null && !guardAsText.trim().equals("")) {
        val guard = mParser.parse(guardAsText, Operator.TYPE_BOOLEAN).asInstanceOf[T]
        return Some(guard)
      }
    } catch { case _: Throwable => println("Problem when parsing guard:\n Guard: " + guardAsText) }
    None
  }

  def parseActionsStr(actionsAsText: String): Option[Set[BinaryExpressionSubject]] = {
    if (actionsAsText != null && !actionsAsText.trim().equals("")) {
      val actions = actionsAsText.split(";").filter(_.length > 0).map(text =>
        try {
          val action = mParser.parse(text).asInstanceOf[SimpleExpressionSubject]
          action match {
            case a: BinaryExpressionSubject => Some(a)
            case _ => throw new TypeMismatchException(action, "ACTION"); None
          }
        } catch { case _: Throwable => println("Problem when parsing action: \n ActionAsText: " + actionsAsText + "\n Action: " + text); return None })
      return Some(actions.flatten.toSet)
    }
    None
  }

  def evalExpression(exp: SimpleExpressionProxy)(state: Map[String, Int]): Option[Boolean] = {
    // Translate state into a bindingcontext
    val mbc = new ModuleBindingContext(mModule)

    state.foreach {
      case (variable, value) => try {
        mbc.insertBinding(mFactory.createSimpleIdentifierProxy(variable), mFactory.createIntConstantProxy(value))
      } catch { case _: Throwable => println("Problem when inserting binding to variable:\n Variable: " + variable); return None }
    }

    // Eval expression
    val sec = new SimpleExpressionCompiler(mFactory, mOptable)

    try {
      sec.eval(exp, mbc).toString() match {
        case "1" => return Some(true)
        case _ => return Some(false)
      } 
    } catch { case _: Throwable => println("Problem when evaluating expression:\n Expression: " + exp); return None }
  }

  // some Semantic check
  def semanticsOK: Boolean = if (getDFA.isEmpty) { println("Problem with the semantics! Probably an undefined value for some variable."); false } else true
}