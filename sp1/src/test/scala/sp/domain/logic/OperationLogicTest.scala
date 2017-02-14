package sp.domain.logic

import org.scalatest._
import sp.domain._

/**
 * Created by Kristofer on 2014-08-05.
 */
//class OperationLogicTest extends WordSpec with Matchers {
//
//  import OperationLogic._
//  import StateVariableLogic._
//
//  private val range = MapPrimitive(Map("start" -> SPAttributeValue(0), "end" -> SPAttributeValue(3), "step" -> SPAttributeValue(1)))
//  private val domain = ListPrimitive(List(StringPrimitive("hej"), StringPrimitive("då")))
//  private val attrD = SPAttributes(Map("domain" -> domain))
//  private val attrR = SPAttributes(Map("range" -> range))
//  private val attrB = SPAttributes(Map("boolean" -> true))
//
//  val sv1 = StateVarInfo(DomainList(List("hej", "då")))
//  val sv2 = StateVarInfo(DomainRange(new Range(0, 3, 1)))
//  val sv3 = StateVarInfo(DomainBool)
//
//  val v1 = Thing("v1").addStateVar(sv2)
//  val v2 = Thing("v2").addStateVar(sv1)
//  val v3 = Thing("v2").addStateVar(sv3)
//
//  val eq = EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0)))
//  val eq1 = EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(1)))
//  val neq = NEQ(SVIDEval(v2.id), ValueHolder(SPAttributeValue("då")))
//
//  private val condAttr = SPAttributes(Map("group" -> "g1", "kind" -> "pre"))
//  private val condAttr2 = SPAttributes(Map("group" -> "g2", "kind" -> "post"))
//  private val condAttr3 = SPAttributes(Map("group" -> "error", "kind" -> "pre"))
//  val pre = PropositionCondition(eq, List(Action(v1.id, ValueHolder(1))), condAttr)
//  val post = PropositionCondition(eq1, List(Action(v1.id, ValueHolder(1))), condAttr2)
//  val error = PropositionCondition(eq, List(Action(v1.id, ValueHolder(10))), condAttr3)
//
//
//  val o1 = Operation("o1", List(pre, post, error))
//
//  val vm = Map(v1.id -> v1.inDomain, v2.id -> v2.inDomain, v3.id -> v1.inDomain, o1.id -> OperationState.inDomain)
//  val state = State(Map(v1.id -> 0, v2.id -> "hej", v3.id -> false, o1.id -> "i"))
//  val statePost = State(Map(v1.id -> 1, v2.id -> "hej", v3.id -> false, o1.id -> "e"))
//  val state2 = State(Map(v1.id -> 2, v2.id -> "då", v3.id -> false, o1.id -> "i"))
//
//
//
//
//  "An Operation" when {
//    "evaluating Condition" should {
//      "return true when true" in {
//        implicit val props = EvaluateProp(vm, Set("g1"))
//        assert(o1.eval(state))
//      }
//      "return true if no conditions are selected" in {
//        implicit val props = EvaluateProp(vm, Set("noCond"))
//        assert(o1.eval(state2))
//      }
//      "return false if not equal" in {
//        implicit val props = EvaluateProp(vm, Set("g1"))
//        assert(!o1.eval(state2))
//      }
//      "return false if not in domain" in {
//        implicit val props = EvaluateProp(vm, Set("error"))
//        assert(!o1.eval(state))
//      }
//      "work with postconditions" in {
//        implicit val props = EvaluateProp(vm, Set("error"), ThreeStateDefinition)
//        assert(o1.eval(statePost))
//      }
//    }
//    "next state" should {
//      "update correct" in {
//        val cond1 = PropositionCondition(eq,
//          List(Action(v1.id, ValueHolder(2)), Action(v2.id, ValueHolder("då"))))
//        val cond2 = PropositionCondition(eq,
//          List(Action(v1.id, ValueHolder(3)), Action(v3.id, ValueHolder(true))))
//        val o2 = Operation("o1", List(cond1, cond2))
//
//        val newvm = vm + (o2.id -> OperationState.inDomain)
//        val newState = state.next(o2.id -> StringPrimitive("i"))
//
//        implicit val props = EvaluateProp(newvm, Set("g1"))
//
//        o2.next(newState) shouldEqual State(Map(v1.id -> 3, v2.id -> "då", v3.id -> true, o2.id -> "f", o1.id -> "i"))
//      }
//    }
//  }
//
//
//}
