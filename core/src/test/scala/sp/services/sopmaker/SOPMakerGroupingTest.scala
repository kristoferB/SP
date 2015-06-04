package sp.services.sopmaker

import org.scalatest._
import sp.domain._
import sp.domain.logic.OperationLogic.EvaluateProp

/**
 * Created by Kristofer on 2014-08-06.
 */
class SOPMakerGroupingTest extends FreeSpec with Matchers with Defs {
  "The SOPMaker" - {
    "when converting for grouping" - {
      "should convert ops to sop " in {
        val sops = makeSOPsFromOpsID(ops)
        sops shouldEqual List(Hierarchy(o1), Hierarchy(o2), Hierarchy(o3), Hierarchy(o4))
      }
    }
    "when identifying group relations" - {
      "should find parallel" in {
        val g1 = Alternative(so1, so2)
        val g2 = Alternative(so3, so4)
        val result = identifySOPRelation(g1,g2, rels)
        result shouldBe a[Parallel]
      }
      "Should find sequence" in {
        val g1 = Alternative(so1, so2)
        val g2 = so3
        val rels = Map(
          o1o2 -> Alternative(so1, so2),
          o1o3 -> Sequence(so1, so3),
          o1o4 -> Sequence(so1, so4),
          o2o3 -> Sequence(so2, so3),
          o2o4 -> Sequence(so2, so4),
          o3o4 -> Sequence(so3, so4)
        )
        val result = identifySOPRelation(g1,g2, rels)
        result shouldBe a[Sequence]
      }
      "Should handle sequence and sometime in sequence" in {
        val g1 = Alternative(so1, so2)
        val g2 = so3
        val rels = Map(
          o1o2 -> Alternative(so1, so2),
          o1o3 -> Sequence(so1, so3),
          o1o4 -> Sequence(so1, so4),
          o2o3 -> SometimeSequence(so2, so3),
          o2o4 -> Sequence(so2, so4),
          o3o4 -> Sequence(so3, so4)
        )
        val result = identifySOPRelation(g1,g2, rels)
        result shouldBe a[Sequence]
      }
      "Should find someTimeSequence" in {
        val g1 = Alternative(so1, so2)
        val g2 = so3
        val rels = Map(
          o1o2 -> Alternative(so1, so2),
          o1o3 -> SometimeSequence(so1, so3),
          o1o4 -> Sequence(so1, so4),
          o2o3 -> SometimeSequence(so2, so3),
          o2o4 -> Sequence(so2, so4),
          o3o4 -> Sequence(so3, so4)
        )
        val result = identifySOPRelation(g1,g2, rels)
        result shouldBe a[SometimeSequence]
      }
      "should return other if not the same relations" in {
        val g1 = Alternative(so1, so2)
        val g2 = Alternative(so3, so4)
        val rels = Map(
          o1o2 -> Alternative(so1, so2),
          o1o3 -> Alternative(so1, so3),
          o1o4 -> Sequence(so1, so4),
          o2o3 -> SometimeSequence(so2, so3),
          o2o4 -> Sequence(so2, so4),
          o3o4 -> Sequence(so3, so4)
        )
        val result = identifySOPRelation(g1,g2, rels)
        result shouldBe a[Other]
      }
      "should do recursive identifying" in {
        val g1 = Alternative(so1, so2)
        val g2 = Alternative(so3, so4)
        val g3 = Alternative(g2)
        val result = identifySOPRelation(g1,g3, rels)
        result shouldBe a[Parallel]
      }
    }
    "when creating parallel groups " - {
      "should group parallel " in {
        val groups = groupify(sops, rels, _.isInstanceOf[Parallel], Parallel.apply)
        groups.head shouldBe a[Parallel]
      }
      "should group parallel 1 " in {
        val groups = groupify(sops, rels, _.isInstanceOf[Parallel], Parallel.apply)
        groups.head.sop.toSet shouldEqual Set(so1, so2, so3, so4)
      }
      "should group parallel 2 " in {
        val rels = Map(
          o1o2 -> Parallel(so1, so2),
          o1o3 -> Parallel(so1, so3),
          o1o4 -> Sequence(so1, so4),
          o2o3 -> Sequence(so2, so3),
          o2o4 -> Sequence(so2, so4),
          o3o4 -> Sequence(so3, so4)
        )
        val groups = groupify(sops, rels, _.isInstanceOf[Parallel], Parallel.apply)

        groups.head.sop.toSet shouldEqual Set(so1, so2, so3)
      }
      "should group parallel 3 " in {
        val rels = Map(
          o1o2 -> Parallel(so1, so2),
          o1o3 -> Sequence(so1, so3),
          o1o4 -> Sequence(so1, so4),
          o2o3 -> Sequence(so2, so3),
          o2o4 -> Sequence(so2, so4),
          o3o4 -> Parallel(so3, so4)
        )
        val groups = groupify(sops, rels, _.isInstanceOf[Parallel], Parallel.apply)
        groups.head.sop.toSet shouldEqual Set(so1, so2)
        groups.tail.head.sop.toSet shouldEqual Set(so3, so4)
      }
      "should group no parallel " in {
        val rels = Map(
          o1o2 -> Sequence(so1, so2),
          o1o3 -> Sequence(so1, so3),
          o1o4 -> Sequence(so1, so4),
          o2o3 -> Sequence(so2, so3),
          o2o4 -> Sequence(so2, so4),
          o3o4 -> Sequence(so3, so4)
        )
        val groups = groupify(sops, rels, _.isInstanceOf[Parallel], Parallel.apply)
        groups should contain theSameElementsAs List(so1, so2, so3, so4)
      }
      "should group recursive" in {
        val rels = Map(
          o1o2 -> Alternative(so1, so2),
          o1o3 -> Parallel(so1, so3),
          o1o4 -> Parallel(so1, so4),
          o2o3 -> Parallel(so2, so3),
          o2o4 -> Parallel(so2, so4),
          o3o4 -> Alternative(so3, so4)
        )
        val g1 = Alternative(so1, so2)
        val g2 = Alternative(so3, so4)
        val g3 = Alternative(g2)
        val sops = List(g1, g3)
        val groups = groupify(sops, rels, _.isInstanceOf[Parallel], Parallel.apply)
        groups.head shouldBe a[Parallel]
      }

    }
    "when creating alternative groups " - {
      "should group alternative " in {
        val rels = Map(
          o1o2 -> Alternative(so1, so2),
          o1o3 -> Alternative(so1, so3),
          o1o4 -> Alternative(so1, so4),
          o2o3 -> Alternative(so2, so3),
          o2o4 -> Alternative(so2, so4),
          o3o4 -> Alternative(so3, so4)
        )
        val groups = groupify(sops, rels, _.isInstanceOf[Alternative], Alternative.apply)
        groups.head shouldBe a [Alternative]
      }
      "should group alternative 1 " in {
        val rels = Map(
          o1o2 -> Alternative(so1, so2),
          o1o3 -> Alternative(so1, so3),
          o1o4 -> Alternative(so1, so4),
          o2o3 -> Alternative(so2, so3),
          o2o4 -> Alternative(so2, so4),
          o3o4 -> Alternative(so3, so4)
        )
        val groups = groupify(sops, rels, _.isInstanceOf[Alternative], Alternative.apply)
        groups.head.sop.toSet shouldEqual Set(so1, so2, so3, so4)
      }
      "should group alternative 2 " in {
        val rels = Map(
          o1o2 -> Alternative(so1, so2),
          o1o3 -> Alternative(so1, so3),
          o1o4 -> Sequence(so1, so4),
          o2o3 -> Sequence(so2, so3),
          o2o4 -> Sequence(so2, so4),
          o3o4 -> Sequence(so3, so4)
        )
        val groups = groupify(sops, rels, _.isInstanceOf[Alternative], Alternative.apply)

        groups.head.sop.toSet shouldEqual Set(so1, so2, so3)
      }
      "should group alternative 3 " in {
        val rels = Map(
          o1o2 -> Alternative(so1, so2),
          o1o3 -> Sequence(so1, so3),
          o1o4 -> Sequence(so1, so4),
          o2o3 -> Sequence(so2, so3),
          o2o4 -> Sequence(so2, so4),
          o3o4 -> Alternative(so3, so4)
        )
        val groups = groupify(sops, rels, _.isInstanceOf[Alternative], Alternative.apply)
        groups.head.sop.toSet shouldEqual Set(so1, so2)
        groups.tail.head.sop.toSet shouldEqual Set(so3, so4)
      }
      "should group no alternative " in {
        val rels = Map(
          o1o2 -> Sequence(so1, so2),
          o1o3 -> Parallel(so1, so3),
          o1o4 -> Sequence(so1, so4),
          o2o3 -> Sequence(so2, so3),
          o2o4 -> Sequence(so2, so4),
          o3o4 -> Sequence(so3, so4)
        )
        val groups = groupify(sops, rels, _.isInstanceOf[Alternative], Alternative.apply)
        groups should contain theSameElementsAs List(so1, so2, so3, so4)
      }

  }
    "When making sops" - {
      "should create parallel and sequences" in {
        val rels = Map(
          o1o2 -> Parallel(so1, so2),
          o1o3 -> Sequence(so1, so3),
          o1o4 -> Sequence(so1, so4),
          o2o3 -> Sequence(so2, so3),
          o2o4 -> Sequence(so2, so4),
          o3o4 -> Sequence(so3, so4)
        )

        val res = makeTheSop(List(o1, o2, o3, o4), rels)
        res.head shouldEqual Sequence(List(Parallel(List(so2,so1):_*), so3, so4):_*)

      }
    }

}
}

trait Defs extends Groupify with MakeASop{


  val o1 = Operation("o1").id
  val o2 = Operation("o2").id
  val o3 = Operation("o2").id
  val o4 = Operation("o2").id
  val so1 = SOP(o1)
  val so2 = SOP(o2)
  val so3 = SOP(o3)
  val so4 = SOP(o4)

  val ops = List(o1,o2,o3,o4)

  val o1o2 = Set(o1,o2)
  val o1o3 = Set(o1,o3)
  val o1o4 = Set(o1,o4)
  val o2o3 = Set(o2,o3)
  val o2o4 = Set(o2,o4)
  val o3o4 = Set(o3,o4)
  val so1o2: Set[SOP] = Set(so1,so2)
  val so1o3: Set[SOP] = Set(so1,so3)
  val so1o4: Set[SOP] = Set(so1,so4)
  val so2o3: Set[SOP] = Set(so2,so3)
  val so2o4: Set[SOP] = Set(so2,so4)
  val so3o4: Set[SOP] = Set(so3,so4)

  val es = EnabledStatesMap(Map())
  val rm = RelationMap(Map(), es)
  val sops = makeSOPsFromOpsID(ops)

  val rels = Map(
          o1o2 -> Parallel(so1, so2),
          o1o3 -> Parallel(so1, so3),
          o1o4 -> Parallel(so1, so4),
          o2o3 -> Parallel(so2, so3),
          o2o4 -> Parallel(so2, so4),
          o3o4 -> Parallel(so3, so4)
        )


}
