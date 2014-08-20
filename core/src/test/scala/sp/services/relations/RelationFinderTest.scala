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
        val res = findASeq(Setup(List(o2, o1), vm, List(), state, _ => false))
        res.seq shouldEqual List(o1, o2)
      }
      "find no seq if no exists" in {
        val res = findASeq(Setup(List(o2, o1), vm, List(), state2, _ => false))
        res.seq shouldEqual List()
      }
      "Stop when goal" in {
        val res = findASeq(Setup(List(o2, o1), vm, List(), state, _(v1.id) == SPAttributeValue(1)))
        res.seq shouldEqual List(o1)
      }
      "Parallel ops" in {
        val ops = (1 to 100) map { i => Operation(i.toString, List(noActionCond))} toList
        val res = findASeq(Setup(ops, vm, List(), state, _ => false))
        res.seq.toSet shouldEqual ops.toSet
      }
    }
    "When findWhenOperationsEnabled" - {
      "it should find relations" in {
        implicit val setup = Setup(List(o2, o1), vm, List(), state, _ => false)
        val res = findWhenOperationsEnabled(10)
        res.map(o2).pre(o1.id) shouldEqual Set(StringPrimitive("f"))
        //res foreach(r =>println(s"${r._1.name} -> ${r._2.init.map(_._2.toString)} "))


      }
      "find paralell relations" in {
        val ops = (1 to 10) map { i => Operation(i.toString, List(noActionCond))} toList
        val res = findWhenOperationsEnabled(10, Set(ops.head))(Setup(ops, vm, List(), state, _ => false))
        res.map(ops.head).pre(ops.tail.head.id) shouldEqual Set(StringPrimitive("i"), StringPrimitive("f"))
      }
      "should only return given ops" in {
        val ops = (1 to 5) map { i => Operation(i.toString, List(noActionCond))} toList
        val res1 = findWhenOperationsEnabled(10, Set(ops.head))(Setup(ops, vm, List(), state, _ => false))
        val res2 = findWhenOperationsEnabled(10)(Setup(ops, vm, List(), state, _ => false))

        res1.map.size shouldEqual 1
        res2.map.size shouldEqual 5
      }
    }
    "When finding operation relations" - {
      "it should find Seqeunce SOP between ops" in {
        implicit val setup = Setup(List(o2, o1), vm, List(), state, _ => false)
        val sm = findWhenOperationsEnabled(10)
        val res = findOperationRelations(sm)
        res.relations(Set(o1.id, o2.id)) shouldEqual Sequence(o1, o2)
      }
      "it should find Paralell SOP between ops" in {
        val ops = (1 to 3) map { i => Operation(i.toString, List(noActionCond))} toList
        val o1 = ops.head
        val o2 = ops.tail.head
        implicit val setup = Setup(List(o1, o2), vm, List(), state, _ => false)
        val sm = findWhenOperationsEnabled(10)
        val res = findOperationRelations(sm)
        res.relations(Set(o1.id, o2.id)) shouldEqual Parallel(o1, o2)
      }

      "it should find Alternative SOP between ops" in {
        val cond = PropositionCondition(
          EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0))),
          List(Action(v1.id, ValueHolder(1))))
        val o1 = Operation("o1", List(cond))
        val o2 = Operation("o2", List(cond))
        implicit val setup = Setup(List(o1, o2), vm, List(), state, _ => false)
        val sm = findWhenOperationsEnabled(10)
        val res = findOperationRelations(sm)
        res.relations(Set(o1.id, o2.id)) shouldEqual Alternative(o1, o2)
      }

      "it should find Sometime in Sequence SOP between ops" in {
        val cond = PropositionCondition(
          EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0))),
          List(Action(v1.id, ValueHolder(1))))
        val cond2 = PropositionCondition(
          EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(1))),
          List(Action(v1.id, ValueHolder(2))))
        val o1 = Operation("o1", List(cond))
        val o2 = Operation("o2", List(cond))
        val o3 = Operation("o3", List(cond2))
        implicit val setup = Setup(List(o1, o2, o3), vm, List(), state, _(v1.id) == SPAttributeValue(2))
        val sm = findWhenOperationsEnabled(10)
        val res = findOperationRelations(sm)
        println(res)
        res.relations(Set(o1.id, o3.id)) shouldEqual SometimeSequence(o1, o3)
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

  val state = State(Map(v1.id -> 0, v2.id -> "hej", v3.id -> false, o1.id -> "i", o2.id -> "i"))
  val state2 = State(Map(v1.id -> 2, v2.id -> "då", v3.id -> false, o1.id -> "i", o2.id -> "i"))

  val vm: Map[ID, StateVariable] =
    Map(v1.id -> v1, v2.id -> v2, v3.id -> v3, o1.id -> o1, o2.id -> o2)
}
