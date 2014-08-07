package sp.services.relations

import org.scalatest._
import sp.domain._
import sp.domain.logic.OperationLogic.EvaluateProp

/**
 * Created by Kristofer on 2014-08-06.
 */
class RelationFinderTest extends FreeSpec with Matchers with Defs {
  "The RelationFinder" - {
    "when finding a path" - {
      "create a seq" in {
        val res = runASeq(List(o2,o1), vm, state, _=> false)
        res.seq shouldEqual List(o1, o2)
      }
      "find no seq if no exists" in {
        val res = runASeq(List(o2,o1), vm, state2, _=> false)
        res.seq shouldEqual List()
      }
      "Stop when goal" in {
        val res = runASeq(List(o2,o1), vm, state, _(v1.id) == SPAttributeValue(1))
        res.seq shouldEqual List(o1)
      }
      "Parallel ops" in {
        val ops = (1 to 100) map {i =>  Operation(i.toString, List(noActionCond))} toList
        val res = runASeq(ops, vm, state, _=>false)
        res.seq.toSet shouldEqual ops.toSet
      }
    }
  }
}

trait Defs extends RelationFinderAlgotithms {
  private val range = MapPrimitive(Map("start" -> SPAttributeValue(0), "end" -> SPAttributeValue(3), "step" -> SPAttributeValue(1)))
  private val domain = ListPrimitive(List(StringPrimitive("hej"), StringPrimitive("då")))
  private val attrD = SPAttributes(Map("domain" -> domain))
  private val attrR = SPAttributes(Map("range" -> range))
  private val attrB = SPAttributes(Map("boolean" -> true))

  val v1 = StateVariable("v1", attrR)
  val v2 = StateVariable("v2", attrD)
  val v3 = StateVariable("v2", attrB)



  val eq = EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0)))
  val neq = NEQ(SVIDEval(v2.id), ValueHolder(SPAttributeValue("då")))

  val o1Cond = PropositionCondition(
    EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0))),
    List(Action(v1.id, ValueHolder(1))))
  val o2Cond = PropositionCondition(
    EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(1))),
    List(Action(v1.id, ValueHolder(2))))
  val noActionCond = PropositionCondition(
    EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0))),
    List())

  val o1 = Operation("o1", List(o1Cond))
  val o2 = Operation("o2", List(o2Cond))

  val state = State(Map(v1.id -> 0, v2.id -> "hej", v3.id -> false, o1.id->"i", o2.id->"i"))
  val state2 = State(Map(v1.id -> 2, v2.id -> "då", v3.id -> false, o1.id->"i", o2.id->"i"))

  val vm: Map[ID, StateVariable] =
    Map(v1.id -> v1, v2.id -> v2, v3.id -> v3, o1.id->o1, o2.id->o2)
}
