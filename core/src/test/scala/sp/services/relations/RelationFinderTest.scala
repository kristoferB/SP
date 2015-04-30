package sp.services.relations

import org.scalatest._
import sp.domain._
import sp.domain.logic.OperationLogic.EvaluateProp
import sp.domain.logic.PropositionConditionLogic

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
    }
//    "When findWhenOperationsEnabled" - {
//      "it should find relations" in {
//        implicit val setup = prepairSetup(Setup(List(o2, o1), vm, List(), state, _ => false))
//        val res = findWhenOperationsEnabled(10)
//        res.map(o2.id).pre(o1.id) shouldEqual Set(StringPrimitive("f"))
//        //res foreach(r =>println(s"${r._1.name} -> ${r._2.init.map(_._2.toString)} "))
//
//
//      }
//      "find paralell relations" in {
//        val ops = (1 to 10) map { i => Operation(i.toString, List(noActionCond))} toList
//        val res = findWhenOperationsEnabled(10, Set(ops.head))(prepairSetup(Setup(ops, vm, List(), state, _ => false)))
//        res.map(ops.head.id).pre(ops.tail.head.id) shouldEqual Set(StringPrimitive("i"), StringPrimitive("f"))
//      }
//      "should only return given ops" in {
//        val ops = (1 to 5) map { i => Operation(i.toString, List(noActionCond))} toList
//        val res1 = findWhenOperationsEnabled(10, Set(ops.head))(prepairSetup(Setup(ops, vm, List(), state, _ => false)))
//        val res2 = findWhenOperationsEnabled(10)(prepairSetup(Setup(ops, vm, List(), state, _ => false)))
//
//        res1.map.size shouldEqual 1
//        res2.map.size shouldEqual 5
//      }
//    }
//    "When finding operation relations" - {
//      "it should find Seqeunce SOP between ops" in {
//        implicit val setup = prepairSetup(Setup(List(o2, o1), vm, List(), state, _ => false))
//        val sm = findWhenOperationsEnabled(10)
//        val res = findOperationRelations(sm)
//        res.relations(Set(o1.id, o2.id)) shouldEqual Sequence(o1, o2)
//      }
//      "it should find Paralell SOP between ops" in {
//        val ops = (1 to 3) map { i => Operation(i.toString, List(noActionCond))} toList
//        val o1 = ops.head
//        val o2 = ops.tail.head
//        implicit val setup = prepairSetup(Setup(List(o1, o2), vm, List(), state, _ => false))
//        val sm = findWhenOperationsEnabled(10)
//        val res = findOperationRelations(sm)
//        res.relations(Set(o1.id, o2.id)) shouldEqual Parallel(o1, o2)
//      }
//
//      "it should find Alternative SOP between ops" in {
//        val cond = PropositionCondition(
//          EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0))),
//          List(Action(v1.id, ValueHolder(1))))
//        val o1 = Operation("o1", List(cond))
//        val o2 = Operation("o2", List(cond))
//        implicit val setup = prepairSetup(Setup(List(o1, o2), vm, List(), state, _ => false))
//        val sm = findWhenOperationsEnabled(10)
//        val res = findOperationRelations(sm)
//        res.relations(Set(o1.id, o2.id)) shouldEqual Alternative(o1, o2)
//      }
//
//      "it should find Sometime in Sequence SOP between ops" in {
//        val cond = PropositionCondition(
//          EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0))),
//          List(Action(v1.id, ValueHolder(1))))
//        val cond2 = PropositionCondition(
//          EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(1))),
//          List(Action(v1.id, ValueHolder(2))))
//        val o1 = Operation("o1", List(cond))
//        val o2 = Operation("o2", List(cond))
//        val o3 = Operation("o3", List(cond2))
//        implicit val setup = prepairSetup(Setup(List(o1, o2, o3), vm, List(), state, _(v1.id) == SPAttributeValue(2)))
//        val sm = findWhenOperationsEnabled(10)
//        val res = findOperationRelations(sm)
//        res.relations(Set(o1.id, o3.id)) shouldEqual SometimeSequence(o1, o3)
//      }
    }

}

trait Defs extends RelationFinderAlgorithms {
  private val range = MapPrimitive(Map("start" -> SPAttributeValue(0), "end" -> SPAttributeValue(3), "step" -> SPAttributeValue(1)))
  private val domain = ListPrimitive(List(StringPrimitive("hej"), StringPrimitive("då")))
  private val attrD = SPAttributes(Map("domain" -> domain))
  private val attrR = SPAttributes(Map("range" -> range))
  private val attrB = SPAttributes(Map("boolean" -> true))

  import sp.domain.logic.StateVariableLogic._
  import sp.domain.logic.OperationLogic._

  val sv1 = StateVarInfo(DomainRange(new Range(0, 10, 1)))
  val sv2 = StateVarInfo(DomainList(List("hej", "då", "klar")))
  val sv3 = StateVarInfo(DomainBool)

  val v1 = Thing("v1").addStateVar(sv1)
  val v2 = Thing("v2").addStateVar(sv2)
  val v3 = Thing("v2").addStateVar(sv3)


  val propParser = sp.domain.logic.PropositionParser(List(v1, v2, v3))
  val actionParser = sp.domain.logic.ActionParser(List(v1, v2, v3))

  implicit val things = List(v1, v2, v3)


  val o1Pre = PropositionCondition("v1 == 0", List("v1 := 1"), SPAttributes(Map("kind"->"precondition")))
  val o1Post = PropositionCondition("v1 == 1", List("v1 = 2"), SPAttributes(Map("kind"->"postcondition")))
  val o1 = Operation("o1", List(o1Pre, o1Post))

  val o2Pre = PropositionCondition("v1 == 2", List("v1 := 3"), SPAttributes(Map("kind"->"precondition")))
  val o2Post = PropositionCondition("v1 == 3", List("v1 = 4"), SPAttributes(Map("kind"->"postcondition")))
  val o2 = Operation("o2", List(o2Pre, o2Post))

  val o3Pre = PropositionCondition("v2 == hej", List(), SPAttributes(Map("kind"->"precondition")))
  val o3Post = PropositionCondition(AlwaysTrue, List("v2 = då"), SPAttributes(Map("kind"->"postcondition")))
  val o3 = Operation("o3", List(o3Pre, o3Post))

  val o4Pre = PropositionCondition("v2 == då && v1 == 2", List("v1 := 4"), SPAttributes(Map("kind"->"precondition")))
  val o4Post = PropositionCondition(AlwaysTrue, List("v2 = klar"), SPAttributes(Map("kind"->"postcondition")))
  val o4 = Operation("o4", List(o4Pre, o4Post))
  



  val state = State(Map(v1.id -> 0, v2.id -> "hej", v3.id -> false, o1.id -> "i", o2.id -> "i", o3.id -> "i", o4.id -> "i"))
  val state2 = State(Map(v1.id -> 2, v2.id -> "då", v3.id -> false, o1.id -> "i", o2.id -> "i", o3.id -> "i", o4.id -> "i"))

  val vm=Map(v1.id -> v1.inDomain, v2.id -> v2.inDomain, v3.id -> v3.inDomain, o1.id -> o1.inDomain, o2.id -> o2.inDomain, o3.id -> o3.inDomain, o4.id -> o4.inDomain)
}
