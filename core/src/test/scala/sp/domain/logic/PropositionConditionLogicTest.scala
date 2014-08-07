package sp.domain.logic

import org.scalatest._
import sp.domain._

/**
 * Created by Kristofer on 2014-08-05.
 */
class PropositionConditionLogicTest extends WordSpec with Matchers {

  import PropositionConditionLogic._

  private val range = MapPrimitive(Map("start"-> SPAttributeValue(0), "end"-> SPAttributeValue(3), "step" ->  SPAttributeValue(1)))
  private val domain = ListPrimitive(List(StringPrimitive("hej"), StringPrimitive("då")))
  private val attrD = SPAttributes(Map("domain"-> domain))
  private val attrR = SPAttributes(Map("range"-> range))
  private val attrB = SPAttributes(Map("boolean"-> true))

  val v1 = StateVariable("v1", attrR)
  val v2 = StateVariable("v2", attrD)
  val v3 = StateVariable("v2", attrB)
  val vm = Map(v1.id->v1, v2.id->v2, v3.id->v3)
  val state = State(Map(v1.id -> 0, v2.id -> "hej", v3.id -> false ))
  val state2 = State(Map(v1.id -> 2, v2.id -> "då", v3.id -> false ))

  val eq = EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0)))
  val neq = NEQ(SVIDEval(v2.id), ValueHolder(SPAttributeValue("då")))

  "A Proposition" when {
    "evaluating EQ" should {
      "return true when equal" in {
        assert(eq.eval(state))
        assert(!eq.eval(state2))
      }
    }
    "evaluating NEQ" should {
      "return true when not egual" in {
        assert(neq.eval(state))
        assert(!neq.eval(state2))
      }
    }

    "evaluating NOT" should {
      "return true when it is not equal" in {
        val not = NOT(eq)
        assert(not.eval(state2))
        assert(!not.eval(state))
      }
    }

    "evaluating AND" should {
      "return true when both are true" in {
        val and1 = AND(List(eq, neq))
        val and2 = AND(List(eq, NOT(neq)))
        assert(and1.eval(state))
        assert(!and1.eval(state2))
        assert(!and2.eval(state))
      }
    }

    "evaluating OR" should {
      "return true when any are true " in {
        val or1 = OR(List(eq, neq))
        val or2 = OR(List(eq, NOT(neq)))
        assert(or1.eval(state))
        assert(!or1.eval(state2))
        assert(or2.eval(state2))
      }
    }
  }




  "A Condition" when {
    "evaluating a state" should {
      "return true when true" in {
        val c = PropositionCondition(eq, List(Action(v1.id, ValueHolder(1))))
        assert(c.eval(state))
        assert(!c.eval(state2))
      }
      "return true when possible to update" in {
        val c = PropositionCondition(eq, List(Action(v1.id, ValueHolder(1))))
        assert(c.inDomain(state, vm))
      }
      "return false when outside of range during update" in {
        val c = PropositionCondition(eq, List(Action(v1.id, ValueHolder(5))))
        assert(!c.inDomain(state, vm))
      }
      "return false when not in domain during update" in {
        val c = PropositionCondition(eq, List(Action(v2.id, ValueHolder("nej"))))
        assert(!c.inDomain(state, vm))
      }
      "return true when in domain during update" in {
        val c = PropositionCondition(eq, List(Action(v2.id, ValueHolder("då"))))
        assert(c.inDomain(state, vm))
      }
      "Boolean in domain during update" in {
        val c = PropositionCondition(eq, List(Action(v3.id, ValueHolder(true))))
        assert(c.inDomain(state, vm))
      }
    }
    "updating a state" should {
      "return correct state" in {
        val newState = State(Map(v1.id -> 1, v2.id -> "då", v3.id -> true ))
        val c = PropositionCondition(eq, List(Action(v1.id, INCR(1)), Action(v2.id, ValueHolder("då")), Action(v3.id, ValueHolder(true))))
        assert(c.next(state) == newState)
      }

      "Action INCR" in {
        val c = PropositionCondition(eq, List(Action(v1.id, INCR(1))))
        assert(c.next(state)(v1.id) == IntPrimitive(1))
      }
      "Action DECR" in {
        val c = PropositionCondition(eq, List(Action(v1.id, DECR(1))))
        assert(c.next(state)(v1.id) == IntPrimitive(-1))
      }
      "Action Assign" in {
        val v4 = StateVariable("v1", attrR)
        val newState = State(Map(v1.id -> 1, v4.id -> 3 , v2.id -> "då", v3.id -> true ))
        val c = PropositionCondition(eq, List(Action(v1.id, ASSIGN(v4.id))))
        assert(c.next(newState)(v1.id) == IntPrimitive(3))
      }
    }
  }

}
