package sp.domain.logic

import org.scalatest._
import sp.domain._

/**
 * Created by kristofer on 05/09/14.
 */
class SOPLogicTest extends FreeSpec with Matchers {

  import SOPLogic._

  val o1 = Operation("o1")
  val o2 = Operation("o2")
  val o3 = Operation("o3")
  val o4 = Operation("o4")
  val o5 = Operation("o5")
  val o6 = Operation("o6")

  val sopSeq = Sequence(o1, o2)
  val sopPar = Parallel(o1, o2)
  val sopAlt = Alternative(o1, o2)
  val sopArb = Arbitrary(o1, o2)
  val sopOther = Other(o1, o2)


  "When extracting guards" - {
    "finding first ops should" - {
      "return empty if empty" in {
        getStartOperations(Parallel()) shouldEqual Set[ID]()
        getStartOperations(EmptySOP) shouldEqual Set[ID]()
        getStartOperations(Arbitrary()) shouldEqual Set[ID]()
        getStartOperations(Alternative()) shouldEqual Set[ID]()
        getStartOperations(Sequence()) shouldEqual Set[ID]()
        getStartOperations(SometimeSequence()) shouldEqual Set[ID]()
        getStartOperations(Other(Parallel(EmptySOP, Alternative()))) shouldEqual Set[ID]()
      }

      "return first in seq" in {
        getStartOperations(sopSeq) shouldEqual Set(o1.id)
        getStartOperations(SometimeSequence(o1, o2)) shouldEqual Set(o1.id)
      }

      "return all in group sops" in {
        getStartOperations(sopPar) shouldEqual Set(o1.id, o2.id)
        getStartOperations(sopAlt) shouldEqual Set(o1.id, o2.id)
        getStartOperations(sopArb) shouldEqual Set(o1.id, o2.id)
        getStartOperations(sopOther) shouldEqual Set(o1.id, o2.id)
      }

      "return first in req seq" in {
        getStartOperations(Sequence(Sequence(sopSeq))) shouldEqual Set(o1.id)
      }

      "retun first in complex struct" in {
        val sop = Parallel(Sequence(o1, o2), Alternative(o3, Arbitrary(Sequence(o4, o5), o6)))
        getStartOperations(sop) shouldEqual Set(o1.id, o3.id, o4.id, o6.id)
      }
    }

    "finding final ops should" - {
      "retun final in complex struct" in {
        val sop = Parallel(Sequence(o1, o2), Alternative(o3, Arbitrary(Sequence(o4, o5), o6)))
        getFinalOperations(sop) shouldEqual Set(o2.id, o3.id, o5.id, o6.id)
      }
    }
    "when calculating complete proposition" - {
      "it should return finish state when op" in {
        val sop = SOP(o1)
        getCompleteProposition(sop) shouldEqual EQ(o1.id, "f")
      }
      "it should return always True when empty" in {
        getCompleteProposition(EmptySOP) shouldEqual AlwaysTrue
        getCompleteProposition(Parallel()) shouldEqual AlwaysTrue
      }
      "it should AND proposition when Parallel" in {
        getCompleteProposition(sopPar) shouldEqual AND(List(EQ(o1.id, "f"), EQ(o2.id, "f")))
      }
      "it should return last proposition when Sequence" in {
        getCompleteProposition(sopSeq) shouldEqual EQ(o2.id, "f")
      }
      "it should AND proposition when various SOPs" in {
        getCompleteProposition(sopArb) shouldEqual AND(List(EQ(o1.id, "f"), EQ(o2.id, "f")))
        getCompleteProposition(sopOther) shouldEqual AND(List(EQ(o1.id, "f"), EQ(o2.id, "f")))
      }
      "it should OR proposition when Alternative SOP" in {
        getCompleteProposition(sopAlt) shouldEqual OR(List(EQ(o1.id, "f"), EQ(o2.id, "f")))
      }
      "it should return proposition when complex SOP" in {
        val sop = Parallel(Sequence(o1, o2), Alternative(o3, Arbitrary(Sequence(o4, o5), o6)))
        getCompleteProposition(sop) shouldEqual AND(List(
          EQ(o2.id, "f"),
            OR(List(EQ(o3.id, "f"),
              AND(List(EQ(o5.id, "f"), EQ(o6.id, "f")))))))
      }
    }
    "when calculating start proposition" - {
      "it should return init state when op" in {
        val sop = SOP(o1)
        getStartProposition(sop) shouldEqual EQ(o1.id, "i")
      }
      "it should return always True when empty" in {
        getStartProposition(EmptySOP) shouldEqual AlwaysTrue
        getStartProposition(Parallel()) shouldEqual AlwaysTrue
      }
      "it should AND proposition when Parallel" in {
        getStartProposition(sopPar) shouldEqual AND(List(EQ(o1.id, "i"), EQ(o2.id, "i")))
      }
      "it should return first proposition when Sequence" in {
        getStartProposition(sopSeq) shouldEqual EQ(o1.id, "i")
      }
      "it should AND proposition when various SOPs" in {
        getStartProposition(sopArb) shouldEqual AND(List(EQ(o1.id, "i"), EQ(o2.id, "i")))
        getStartProposition(sopOther) shouldEqual AND(List(EQ(o1.id, "i"), EQ(o2.id, "i")))
      }
      "it should OR proposition when Alternative SOP" in {
        getStartProposition(sopAlt) shouldEqual AND(List(EQ(o1.id, "i"), EQ(o2.id, "i")))
      }
      "it should return proposition when complex SOP" in {
        val sop = Parallel(Sequence(o1, o2), Alternative(o3, Arbitrary(Sequence(o4, o5), o6)))
        getStartProposition(sop) shouldEqual AND(List(
          EQ(o1.id, "i"),
            AND(List(EQ(o3.id, "i"),
              AND(List(EQ(o4.id, "i"), EQ(o6.id, "i")))))))
      }
    }
    "when updating a map it should" - {
      "add new ids to map" in {
        val oldMap: Map[ID, Set[Proposition]] = Map(o1.id -> Set(AlwaysTrue))
        val newMap: Map[ID, Set[Proposition]] = Map(o2.id -> Set(AlwaysTrue))
        updateMap(newMap, oldMap) shouldEqual Map(o1.id -> Set(AlwaysTrue), o2.id -> Set(AlwaysTrue))
      }
    }
    "when finding propositions it should" - {
      "no props when empty" in {
        findOpProps(Parallel(), Map()) shouldEqual Map()
        findOpProps(Sequence(), Map()) shouldEqual Map()
      }
      "add props in each Alternative" in {
        val propMap = Map(o1.id -> Set(EQ(o2.id, "i")), o2.id -> Set(EQ(o1.id, "i")))
        findOpProps(sopAlt, Map()) shouldEqual propMap
      }
      "add no props in each Parallel" in {
        findOpProps(sopPar, Map()) shouldEqual Map()
      }
      "add props in each Arbitrary" in {
        val propMap = Map(o1.id -> Set(OR(List(EQ(o2.id, "i"), EQ(o2.id, "f")))), o2.id -> Set(OR(List(EQ(o1.id, "i"), EQ(o1.id, "f")))))
        findOpProps(sopArb, Map()) shouldEqual propMap
      }
      "add props in each Arbitrary 2" in {
        val propMap = Map(o1.id -> Set(OR(List(EQ(o3.id, "i"), EQ(o3.id, "f")))),
          o3.id -> Set(OR(List(EQ(o1.id, "i"), EQ(o2.id, "f")))),
          o2.id -> Set(EQ(o1.id, "f")))
        findOpProps(Arbitrary(Sequence(o1, o2), o3), Map()) shouldEqual propMap
      }
      "add props in each Sequence" in {
        val propMap = Map(o2.id -> Set(EQ(o1.id, "f")))
        findOpProps(sopSeq, Map()) shouldEqual propMap
      }
      "add props in each Sequence 2" in {
        val propMap = Map(o2.id -> Set(EQ(o1.id, "f")), o3.id -> Set(EQ(o2.id, "f")))
        findOpProps(Sequence(o1, o2, o3), Map()) shouldEqual propMap
      }
      "add props to all ops in a group in a Sequence" in {
        val propMap = Map(
          o2.id -> Set(EQ(o1.id, "f")),
          o3.id -> Set(EQ(o1.id, "f")),
          o4.id -> Set(AND(List(EQ(o2.id, "f"), EQ(o3.id, "f"))))
        )
        findOpProps(Sequence(o1, Parallel(o2, o3), o4), Map()) shouldEqual propMap
      }
      "it should add proposition when complex SOP" in {
        val sop = Parallel(Sequence(o1, o2), Alternative(o3, Arbitrary(Sequence(o4, o5), o6)))
        val propMap = Map(
          o2.id -> Set(EQ(o1.id, "f")),
          o5.id -> Set(EQ(o4.id, "f")),
          o3.id -> Set(AND(List(EQ(o4.id, "i"), EQ(o6.id, "i")))),
          o4.id -> Set(EQ(o3.id, "i"), OR(List(EQ(o6.id, "i"), EQ(o6.id, "f")))),
          o6.id -> Set(EQ(o3.id, "i"), OR(List(EQ(o4.id, "i"), EQ(o5.id, "f"))))
        )
        findOpProps(sop, Map()) shouldEqual propMap
      }
      "it should add proposition when complex SOP 2" in {
        val sop = Alternative(o3, Arbitrary(Sequence(o4, o5), o6))
        val propMap = Map(
          o5.id -> Set(EQ(o4.id, "f")),
          o3.id -> Set(AND(List(EQ(o4.id, "i"), EQ(o6.id, "i")))),
          o4.id -> Set(EQ(o3.id, "i"), OR(List(EQ(o6.id, "i"), EQ(o6.id, "f")))),
          o6.id -> Set(EQ(o3.id, "i"), OR(List(EQ(o4.id, "i"), EQ(o5.id, "f"))))
        )
        val res = findOpProps(sop, Map())
        res shouldEqual propMap

//        println(s"o6: ${res(o6.id)}")
//        println(s"o6*: ${propMap(o6.id)}")
//        println(s"equal: ${res(o6.id) == propMap(o6.id)}")

      }

    }

    "When extracting operation condition" - {
      "it should return empty map" in {
        extractOperationCondition(EmptySOP, "g1") shouldEqual Map()
      }
      "it should return a condition for sequence" in {
        val expectedRes = Map(o2.id -> PropositionCondition(
          EQ(o1.id, "f"), List(), SPAttributes(Map("group" -> "g1", "kind"-> "precondition")))
        )
        extractOperationCondition(sopSeq, "g1") shouldEqual expectedRes
      }
      "it should return a condition for alternatives" in {
        val expectedRes = Map(
          o2.id -> PropositionCondition(EQ(o1.id, "i"), List(), SPAttributes(Map("group" -> "g1", "kind"-> "precondition"))),
          o1.id -> PropositionCondition(EQ(o2.id, "i"), List(), SPAttributes(Map("group" -> "g1", "kind"-> "precondition")))
        )
        extractOperationCondition(sopAlt, "g1") shouldEqual expectedRes
      }
    }

    "When extracting all operations" - {
      "it should return empty map" in {
        getAllOperations(EmptySOP) shouldEqual Set()
      }
      "it should return all in seq" in {
        getAllOperations(sopSeq) shouldEqual Set(o1.id, o2.id)
      }
      "it should return all in seq 2" in {
        getAllOperations(SometimeSequence(o1, o2, o3, o4)) shouldEqual Set(o1.id, o2.id, o3.id, o4.id)
      }
      "it should return all ops in complex" in {
        val sop = Parallel(Sequence(o1, o2), Alternative(o3, Arbitrary(Sequence(o4, o5), o6)))
        getAllOperations(sop) shouldEqual Set(o1.id, o2.id, o3.id, o4.id, o5.id, o6.id)
      }
    }



    "when finding propositions and adding to all, it should" - {
      "no props when empty" in {
        findOpProps(Parallel(), Map(), true) shouldEqual Map()
        findOpProps(Sequence(), Map(), true) shouldEqual Map()
      }
      "add props in each Alternative" in {
        val propMap = Map(o1.id -> Set(EQ(o2.id, "i")), o2.id -> Set(EQ(o1.id, "i")))
        findOpProps(sopAlt, Map(), true) shouldEqual propMap
      }
      "add no props in each Parallel" in {
        findOpProps(sopPar, Map(), true) shouldEqual Map()
      }
      "add props in each Arbitrary" in {
        val propMap = Map(o1.id -> Set(OR(List(EQ(o2.id, "i"), EQ(o2.id, "f")))), o2.id -> Set(OR(List(EQ(o1.id, "i"), EQ(o1.id, "f")))))
        findOpProps(sopArb, Map(), true) shouldEqual propMap
      }
      "add props in each Arbitrary 2" in {
        val propMap = Map(o1.id -> Set(OR(List(EQ(o3.id, "i"), EQ(o3.id, "f")))),
          o3.id -> Set(OR(List(EQ(o1.id, "i"), EQ(o2.id, "f")))),
          o2.id -> Set(EQ(o1.id, "f"), OR(List(EQ(o3.id, "i"), EQ(o3.id, "f"))))
        )
        findOpProps(Arbitrary(Sequence(o1, o2), o3), Map(), true) shouldEqual propMap
      }
      "add props in each Sequence" in {
        val propMap = Map(o2.id -> Set(EQ(o1.id, "f")))
        findOpProps(sopSeq, Map()) shouldEqual propMap
      }
      "add props in each Sequence 2" in {
        val propMap = Map(o2.id -> Set(EQ(o1.id, "f")), o3.id -> Set(AND(List(EQ(o1.id, "f"), EQ(o2.id, "f")))))
        findOpProps(Sequence(o1, o2, o3), Map(), true) shouldEqual propMap
      }
//      "it should add proposition when complex SOP" in {
//        val sop = Parallel(Sequence(o1, o2), Alternative(o3, Arbitrary(Sequence(o4, o5), o6)))
//        val propMap = Map(
//          o2.id -> Set(EQ(o1.id, "f")),
//          o5.id -> Set(EQ(o4.id, "f")),
//          o3.id -> Set(AND(List(EQ(o4.id, "i"), EQ(o6.id, "i")))),
//          o4.id -> Set(EQ(o3.id, "i"), OR(List(EQ(o6.id, "i"), EQ(o6.id, "f")))),
//          o6.id -> Set(EQ(o3.id, "i"), OR(List(EQ(o4.id, "i"), EQ(o5.id, "f"))))
//        )
//        findOpProps(sop, Map()) shouldEqual propMap
//      }
//      "it should add proposition when complex SOP 2" in {
//        val sop = Alternative(o3, Arbitrary(Sequence(o4, o5), o6))
//        val propMap = Map(
//          o5.id -> Set(EQ(o4.id, "f")),
//          o3.id -> Set(AND(List(EQ(o4.id, "i"), EQ(o6.id, "i")))),
//          o4.id -> Set(EQ(o3.id, "i"), OR(List(EQ(o6.id, "i"), EQ(o6.id, "f")))),
//          o6.id -> Set(EQ(o3.id, "i"), OR(List(EQ(o4.id, "i"), EQ(o5.id, "f"))))
//        )
//        val res = findOpProps(sop, Map())
//        res shouldEqual propMap
//
//        //        println(s"o6: ${res(o6.id)}")
//        //        println(s"o6*: ${propMap(o6.id)}")
//        //        println(s"equal: ${res(o6.id) == propMap(o6.id)}")
//
//      }

    }

  }



}
