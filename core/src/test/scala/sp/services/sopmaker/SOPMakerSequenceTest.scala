package sp.services.sopmaker

import org.scalatest._
import sp.domain._
import sp.domain.logic.OperationLogic.EvaluateProp

/**
 * Created by Kristofer on 2014-08-06.
 */
class SOPMakerSequenceTest extends FreeSpec with Matchers with Defs2 {
  "The SOPMaker" - {
    "when aligning" - {
      "should checkIfSeq returns correct " in {
        assert(checkIfSeq(so1, so3, Sequence(so1, so3)))
        assert(!checkIfSeq(so3, so1, Sequence(so1, so3)))
        assert(checkIfSeq(so1, so3, SometimeSequence(so1, so3)))
        assert(!checkIfSeq(so3, so1, SometimeSequence(so1, so3)))
      }
      "should return empty node if seq is empty" in {
        val res = align(Seq(), Map())
        res shouldBe emptyNode
      }
      "should return no seq if no seq" in {
        val rels: Map[Set[SOP], SOP] = Map(
          so1o2 -> Parallel(so1, so2),
          so1o3 -> Parallel(so1, so3),
          so1o4 -> Parallel(so1, so4),
          so2o3 -> Parallel(so2, so3),
          so2o4 -> Parallel(so2, so4),
          so3o4 -> Parallel(so3, so4)
        )

        val res = align(Seq(so1, so2, so3, so4), rels, so1)
        res.s shouldBe so1
        List(res.other.s) should contain oneOf (so2, so3, so4)
        List(res.other.other.s) should contain oneOf (so2, so3, so4)
        List(res.other.other.other.s) should contain oneOf (so2, so3, so4)
      }
      "should use base" in {
        val res = align(Seq(so1, so2, so3, so4), rels, so4)
        res.s shouldBe so4
      }
      "should sort the aligned nodes" in {
        val rels: Map[Set[SOP], SOP] = Map(
          so1o2 -> Sequence(so1, so2),
          so1o3 -> Sequence(so1, so3),
          so1o4 -> Sequence(so1, so4),
          so2o3 -> Sequence(so2, so3),
          so2o4 -> Sequence(so2, so4),
          so3o4 -> Sequence(so3, so4)
        )
        val res = align(Seq(so1, so2, so3, so4), rels, so1)
        val sorted = sortNodes(res)
        opSeq(sorted.head) shouldEqual "o1o2o3o4"
      }
      "should creat seq 1" in {
        val rels: Map[Set[SOP], SOP] = Map(
          so1o2 -> Sequence(so1, so2),
          so1o3 -> Parallel(so1, so3),
          so1o4 -> Parallel(so1, so4),
          so2o3 -> Parallel(so2, so3),
          so2o4 -> Parallel(so2, so4),
          so3o4 -> Sequence(so3, so4)
        )
        val res = align(Seq(so1, so2, so3, so4), rels, so1)
        val sorted = sortNodes(res) map opSeq
        sorted should contain allOf ("o1o2", "o3o4")
      }
      "should creat seq 2" in {
        val rels: Map[Set[SOP], SOP] = Map(
          so1o2 -> SometimeSequence(so1, so2),
          so1o3 -> Parallel(so1, so3),
          so1o4 -> Parallel(so1, so4),
          so2o3 -> Parallel(so2, so3),
          so2o4 -> Parallel(so2, so4),
          so3o4 -> SometimeSequence(so3, so4)
        )
        val res = align(Seq(so1, so2, so3, so4), rels, so1)
        val sorted = sortNodes(res) map opSeq
        sorted should contain allOf ("o1o2", "o3o4")
      }
    }
    "when sopifying" - {
      "should return empty if empty input" in {
        val node = emptyNode
        val res = sopify(node, Map())
        res shouldEqual List(EmptySOP)
      }
      "should return return single sop" in {
        val node = Node(so1, emptyNode, emptyNode, emptyNode)
        val res = sopify(node, Map())
        res shouldEqual List(so1)
      }
      "should return return sequence sop" in {
        val rels: Map[Set[SOP], SOP] = Map(
          so1o2 -> Sequence(so1, so2),
          so1o3 -> Parallel(so1, so3),
          so1o4 -> Parallel(so1, so4),
          so2o3 -> Parallel(so2, so3),
          so2o4 -> Parallel(so2, so4),
          so3o4 -> SometimeSequence(so3, so4)
        )

        val node2 = Node(so2, emptyNode, emptyNode, emptyNode)
        val node = Node(so1, emptyNode, node2, emptyNode)
        val res = sopify(node, rels)
        res shouldEqual List(Sequence(so1, so2))
      }
      "should return sometime sequence sop" in {
        val rels: Map[Set[SOP], SOP] = Map(
          so1o2 -> Sequence(so1, so2),
          so1o3 -> Sequence(so1, so3),
          so1o4 -> Sequence(so1, so4),
          so2o3 -> SometimeSequence(so2, so3),
          so2o4 -> Sequence(so2, so4),
          so3o4 -> Sequence(so3, so4)
        )

        val node4 = Node(so4, emptyNode, emptyNode, emptyNode)
        val node3 = Node(so3, emptyNode, node4, emptyNode)
        val node2 = Node(so2, emptyNode, node3, emptyNode)
        val node = Node(so1, emptyNode, node2, emptyNode)
        val res = sopify(node, rels)
        res shouldEqual List(Sequence(so1, SometimeSequence(so2, so3), so4))
      }
      "should return return sometime sequence sop 2" in {
        val rels: Map[Set[SOP], SOP] = Map(
          so1o2 -> Sequence(so1, so2),
          so1o3 -> Sequence(so1, so3),
          so1o4 -> Sequence(so1, so4),
          so2o3 -> SometimeSequence(so2, so3),
          so2o4 -> SometimeSequence(so2, so4),
          so3o4 -> SometimeSequence(so3, so4)
        )

        val node4 = Node(so4, emptyNode, emptyNode, emptyNode)
        val node3 = Node(so3, emptyNode, node4, emptyNode)
        val node2 = Node(so2, emptyNode, node3, emptyNode)
        val node = Node(so1, emptyNode, node2, emptyNode)
        val res = sopify(node, rels)
        res shouldEqual List(Sequence(so1, SometimeSequence(so2, so3, so4)))
      }
      "should return return sometime sequence sop 3" in {
        val rels: Map[Set[SOP], SOP] = Map(
          so1o2 -> SometimeSequence(so1, so2),
          so1o3 -> SometimeSequence(so1, so3),
          so1o4 -> SometimeSequence(so1, so4),
          so2o3 -> SometimeSequence(so2, so3),
          so2o4 -> SometimeSequence(so2, so4),
          so3o4 -> SometimeSequence(so3, so4)
        )

        val node4 = Node(so4, emptyNode, emptyNode, emptyNode)
        val node3 = Node(so3, emptyNode, node4, emptyNode)
        val node2 = Node(so2, emptyNode, node3, emptyNode)
        val node = Node(so1, emptyNode, node2, emptyNode)
        val res = sopify(node, rels)
        res shouldEqual List(SometimeSequence(so1, so2, so3, so4))
      }
    }
    "when sequencifying" - {
      val rels: Map[Set[ID], SOP] = Map(
        o1o2 -> Sequence(so1, so2),
        o1o3 -> Sequence(so1, so3),
        o1o4 -> Sequence(so1, so4),
        o2o3 -> Sequence(so2, so3),
        o2o4 -> Sequence(so2, so4),
        o3o4 -> Sequence(so3, so4)
      )
      "return the same if no sequences" in {
        val rels: Map[Set[ID], SOP] = Map(
          o1o2 -> Parallel(so1, so2),
          o1o3 -> Parallel(so1, so3),
          o1o4 -> Parallel(so1, so4),
          o2o3 -> Parallel(so2, so3),
          o2o4 -> Parallel(so2, so4),
          o3o4 -> Parallel(so3, so4)
        )
        val res = sequencify(List(so1, so2, so3, so4), rels, so1)
        res should contain allOf (so1, so2, so3, so4)
      }
      "return a straight sequence" in {
        printOps
        val res = sequencify(List(so1, so2, so3, so4), rels, so1)
        res shouldEqual List(Sequence(List(so1, so2, so3, so4):_*))
      }
      "return the independent sequences" in {
        val rels: Map[Set[ID], SOP] = Map(
          o1o2 -> Sequence(so1, so2),
          o1o3 -> Parallel(so1, so3),
          o1o4 -> Parallel(so1, so4),
          o2o3 -> Parallel(so2, so3),
          o2o4 -> Parallel(so2, so4),
          o3o4 -> Sequence(so3, so4)
        )
        val res = sequencify(List(so1, so2, so3, so4), rels, so1)
        res should contain allOf (
          Sequence(List(so1, so2):_*),
          Sequence(List(so3, so4):_*)
          )

      }
    }
    "testing" in {
      val rels: Map[Set[ID], SOP] = Map(
        o1o2 -> Sequence(so1, so2),
        o1o3 -> Sequence(so1, so3),
        o1o4 -> Parallel(so1, so4),
        Set(o1, o5) -> Sequence(so1, so5),
        Set(o1, o6) -> Sequence(so1, so6),
        o2o3 -> Sequence(so2, so3),
        o2o4 -> Parallel(so2, so4),
        Set(o2, o5) -> Sequence(so2, so5),
        Set(o2, o6) -> Sequence(so2, so6),
        Set(o3, o4) -> Parallel(so3, so4),
        Set(o3, o5) -> Parallel(so3, so5),
        Set(o3, o6) -> Parallel(so3, so6),
        Set(o4, o5) -> Sequence(so4, so5),
        Set(o4, o6) -> Sequence(so4, so6),
        Set(o5, o6) -> Sequence(so5, so6)
      )

      val allOps = ops ++ List(o4, o5, o6)
      val algo = new MakeASop{}
      val res = algo.makeTheSop(allOps, rels, o4)

      println(s"${op1.name} $o1")
      println(s"${op2.name} $o2")
      println(s"${op3.name} $o3")
      println(s"${op4.name} $o4")
      println(s"${op5.name} $o5")
      println(s"${op6.name} $o6")

      println(s"NOW I GOT \n $res")

      import sp.domain.logic.SOPLogic._
      println(extractRelations(res))

    }
  }
}

trait Defs2 extends Sequencify with Groupify {

  val op1 = Operation("o1")
  val op2 = Operation("o2")
  val op3 = Operation("o3")
  val op4 = Operation("o4")
  val op5 = Operation("o5")
  val op6 = Operation("o6")

  val o1 = op1.id
  val o2 = op2.id
  val o3 = op3.id
  val o4 = op4.id
  val o5 = ID.makeID("dc732cfc-dac1-426d-a7f5-e49094b8995e").get
  val o6 = op6.id
  val so1 = SOP(o1)
  val so2 = SOP(o2)
  val so3 = SOP(o3)
  val so4 = SOP(o4)
  val so5 = SOP(o5)
  val so6 = SOP(o6)

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

  val rels: Map[Set[SOP], SOP] = Map(
    so1o2 -> Sequence(so1, so2),
    so1o3 -> Sequence(so1, so3),
    so1o4 -> Sequence(so1, so4),
    so2o3 -> Sequence(so2, so3),
    so2o4 -> Sequence(so2, so4),
    so3o4 -> Sequence(so3, so4)
  )

  def printOps = {
    println(s"op1: $o1")
    println(s"op2: $o2")
    println(s"op3: $o3")
    println(s"op4: $o4")
  }

  def getOpName(id: ID): String = {
    id match {
      case x if x == o1 => "o1"
      case x if x == o2 => "o2"
      case x if x == o3 => "o3"
      case x if x == o4 => "o4"
      case _ => "no match " + id
    }
  }

  def getOpName(s: SOP): String = {
    s match {
      case h: Hierarchy => getOpName(h.operation)
      case _ => s.sop.foldLeft("")(_ + "_" +getOpName(_))
    }
  }

  def opSeq(ns: Seq[Node]) = {
    ns.foldLeft("")((res, node) => res + getOpName(node.s))
  }





}
