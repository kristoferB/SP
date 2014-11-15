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
        val res = findASeq(prepairSetup(Setup(List(o2, o1), vm, List(), state, _ => false)))
        res should be ('right)
        res.right map(x => x.seq shouldEqual List(o1, o2))
      }
      "find no seq if no exists" in {
        val res = findASeq(prepairSetup(Setup(List(o2, o1), vm, List(), state2, _ => false)))
        res should be ('left)
      }
      "Stop when goal" in {
        val res = findASeq(prepairSetup(Setup(List(o2, o1), vm, List(), state, _(v1.id) == SPAttributeValue(1))))
        res.seq shouldEqual List(o1)
      }
      "Parallel ops" in {
        val ops = (1 to 100) map { i => Operation(i.toString, List(noActionCond))} toList
        val res = findASeq(prepairSetup(Setup(ops, vm, List(), state, _ => false)))
        res.seq.toSet shouldEqual ops.toSet
      }
    }
    "When findWhenOperationsEnabled" - {
      "it should find relations" in {
        implicit val setup = prepairSetup(Setup(List(o2, o1), vm, List(), state, _ => false))
        val res = findWhenOperationsEnabled(10)
        res.map(o2.id).pre(o1.id) shouldEqual Set(StringPrimitive("f"))
        //res foreach(r =>println(s"${r._1.name} -> ${r._2.init.map(_._2.toString)} "))


      }
      "find paralell relations" in {
        val ops = (1 to 10) map { i => Operation(i.toString, List(noActionCond))} toList
        val res = findWhenOperationsEnabled(10, Set(ops.head))(prepairSetup(Setup(ops, vm, List(), state, _ => false)))
        res.map(ops.head.id).pre(ops.tail.head.id) shouldEqual Set(StringPrimitive("i"), StringPrimitive("f"))
      }
      "should only return given ops" in {
        val ops = (1 to 5) map { i => Operation(i.toString, List(noActionCond))} toList
        val res1 = findWhenOperationsEnabled(10, Set(ops.head))(prepairSetup(Setup(ops, vm, List(), state, _ => false)))
        val res2 = findWhenOperationsEnabled(10)(prepairSetup(Setup(ops, vm, List(), state, _ => false)))

        res1.map.size shouldEqual 1
        res2.map.size shouldEqual 5
      }
    }
    "When finding operation relations" - {
      "it should find Seqeunce SOP between ops" in {
        implicit val setup = prepairSetup(Setup(List(o2, o1), vm, List(), state, _ => false))
        val sm = findWhenOperationsEnabled(10)
        val res = findOperationRelations(sm)
        res.relations(Set(o1.id, o2.id)) shouldEqual Sequence(o1, o2)
      }
      "it should find Paralell SOP between ops" in {
        val ops = (1 to 3) map { i => Operation(i.toString, List(noActionCond))} toList
        val o1 = ops.head
        val o2 = ops.tail.head
        implicit val setup = prepairSetup(Setup(List(o1, o2), vm, List(), state, _ => false))
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
        implicit val setup = prepairSetup(Setup(List(o1, o2), vm, List(), state, _ => false))
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
        implicit val setup = prepairSetup(Setup(List(o1, o2, o3), vm, List(), state, _(v1.id) == SPAttributeValue(2)))
        val sm = findWhenOperationsEnabled(10)
        val res = findOperationRelations(sm)
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

  import sp.domain.logic.StateVariableLogic._
  import sp.domain.logic.OperationLogic._

  val sv1 = StateVarInfo(DomainList(List("hej", "då")))
  val sv2 = StateVarInfo(DomainRange(new Range(0, 3, 1)))
  val sv3 = StateVarInfo(DomainBool)

  val v1 = Thing("v1").addStateVar(sv2)
  val v2 = Thing("v2").addStateVar(sv1)
  val v3 = Thing("v2").addStateVar(sv3)


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

  val vm=Map(v1.id -> v1.inDomain, v2.id -> v2.inDomain, v3.id -> v3.inDomain, o1.id -> o1.inDomain, o2.id -> o2.inDomain)
}
