package sp.services.sopmaker

import akka.actor._
import sp.domain
import sp.domain._
import sp.domain.logic.SOPLogic

import scala.annotation.tailrec

/**
 * This message starts the identification. Returns the
 * relations identified.
 * TODO: I need to update this later for better performance. Mainly using
 * ints and arrays instead of all the objects.
 *
 * @param ops The ops should have all conditions
 *            that should be used. So add Specs
 *            before
 */
case class MakeMeASOP(ops: List[ID], relations: RelationMap, base: Option[ID])

class SOPMaker extends Actor with MakeASop {
  def receive = {
    case MakeMeASOP(ops, rels, base) => {
      val reply = sender
      val baseSop = (base map SOP.apply).getOrElse(EmptySOP)
      val sop = makeTheSop(ops, rels.relations, baseSop)
      reply ! sop
    }
  }
}

object SOPMaker {
  def props = Props(classOf[SOPMaker])
}

trait MakeASop {
  import SOPLogic._

  def groupThem(sops: List[SOP], relations: Map[Set[ID], SOP]) = {
    val groupOthers = groupify(sops, relations, _.isInstanceOf[Other], Other.apply)

    val groupAlternatives = groupify(groupOthers, relations, _.isInstanceOf[Alternative], Alternative.apply)
    groupify(groupAlternatives, relations, x => x.isInstanceOf[Parallel] || x.isInstanceOf[Arbitrary], Parallel.apply)
  }

  def cleanSop(sop: SOP): SOP = {
    sop match {
      case EmptySOP => EmptySOP
      case x: Parallel if x.sop.size == 1 =>
        x.sop.head match {
          case y: Parallel => cleanSop(x.modify(y.sop))
          case _ => sop.modify(sop.sop.map(cleanSop))
        }
      case _ => sop.modify(sop.sop.map(cleanSop))
    }
  }

  def makeTheSop(ops: List[ID], relations: Map[Set[ID], SOP], base: SOP = EmptySOP) = {
    val sopOps = makeSOPsFromOpsID(ops)

    val groupParallel = groupThem(sopOps, relations)
    val result = sequencify(groupParallel, relations, base)

    val withMissing = addMissingRelations(result, relations)
    withMissing.map(cleanSop)
  }

  def makeSOPsFromOpsID(ops: List[ID]): List[SOP] = ops map SOP.apply

  /**
   * Identifies the relation between two SOPs.
   * @param sop1
   * @param sop2
   * @param relations The SOP relation map containing relation among operations
   * @return a sop containing the sops
   */
  def identifySOPRelation(sop1: SOP, sop2: SOP, relations: Map[Set[ID], SOP]): SOP = {
    val sop1s = extractOps(sop1)
    val sop2s = extractOps(sop2)

    val relationBetweenPairs = (for {
      s1 <- sop1s
      s2 <- sop2s
    } yield {
      if (s1 == s2) Other()
      else {
        relations(Set(s1, s2)) match {
          case x: Sequence => if (x.sop.head.asInstanceOf[Hierarchy].operation == s1) Sequence(sop1, sop2) else Sequence(sop2, sop1)
          case x: SometimeSequence => if (x.sop.head.asInstanceOf[Hierarchy].operation == s1) SometimeSequence(sop1, sop2) else SometimeSequence(sop2, sop1)
          case x: SOP => {x.modify(List(sop1, sop2)) }
        }
      }
    }).toSet

    relationBetweenPairs.toList match {
      case x :: Nil => x
      case x :: y :: Nil if instanceOfSequence(x, y) => Sequence(x.sop.head, x.sop.tail.head)
      case _ => Other(sop1, sop2)
    }

  }

  def extractOps(sop: SOP): List[ID] = {
    sop match {
      case x: Hierarchy => List(x.operation) // does not need to dig since the op is included in the relationMap
      case x: SOP => x.sop flatMap extractOps toList
    }
  }

  private def instanceOfSequence(s1: SOP, s2: SOP): Boolean = {
    (s1.isInstanceOf[Sequence] && s2.isInstanceOf[SometimeSequence] ||
    s1.isInstanceOf[SometimeSequence] && s2.isInstanceOf[Sequence]) &&
    s1.sop.head == s2.sop.head
  }



  def groupify(sopsToGroup: List[SOP],
               relations: Map[Set[ID], SOP],
               relationToGroup: SOP => Boolean,
               createSOP: List[SOP] => SOP): List[SOP] = {

    val sops = sopsToGroup map { sop => if (sop.sop.isEmpty) sop else sop.modify(groupify(sop.sop.toList, relations, relationToGroup, createSOP))}

    val relatedPairs = for {
      x <- sops
      y <- sops if x!=y && relationToGroup(identifySOPRelation(x, y, relations))
    } yield Set(x, y)


    def mergeTheGroups(theGroups: Set[Set[SOP]]): Set[Set[SOP]] = {
      val merge = theGroups.foldLeft(Set[Set[SOP]]())({
        (b, a) => {
          val filter = b partition (_.intersect(a).nonEmpty)
          val union = a ++ filter._1.foldLeft(Set[SOP]())(_ ++ _)
          filter._2 + union
        }
      })
      if (merge != theGroups) mergeTheGroups(merge)
      else merge
    }
    val mergeIntoGroups = mergeTheGroups(relatedPairs.toSet)

    val sopsAddedToGroup = relatedPairs.foldLeft(Set[SOP]())((a, b) => a ++ b)
    val sopsNotAddedToGroup = sops filter (!sopsAddedToGroup.contains(_))

    val newGroups = mergeIntoGroups.map(set => createSOP(set.toList)).toList

    newGroups ++ sopsNotAddedToGroup

  }

  case class Node(s: SOP, pre: Node, succ: Node, other: Node)
  object emptyNode extends Node(null, null, null, null) {
    override def toString = "emptyNode"
  }

  def sequencify(sops: Seq[SOP], relations: Map[Set[ID], SOP], base: SOP = EmptySOP): List[SOP] = {
    val updSops = sops map{
      case s: SOP if s.isEmpty => s
      case s: SOP => s.modify(sequencify(s.sop, relations, base))
    }

    val nodeRelations = (for {
      x <- updSops
      y <- updSops if x != y
    } yield Set(x, y) -> identifySOPRelation(x, y, relations)).toMap

    val node = align(updSops, nodeRelations, relations, base)
    sopify(node, relations) toList
  }

  def align(nodes: Seq[SOP], rel: Map[Set[SOP], SOP], relations: Map[Set[ID], SOP], base: SOP = EmptySOP) : Node = {
    nodes.toList match {
      case Nil => emptyNode
      case EmptySOP :: Nil => emptyNode
      case x :: Nil => Node(x, emptyNode, emptyNode, emptyNode)
      case x :: xs => {
        // here we need a heuristic to guide the sequences. i.e. maybe not use x as base
        // or maybe the user can add the base first when sending in the SOP?
        val gotBase = nodes.contains(base)
        val set = if (gotBase) nodes.filter(_ != base) else xs
        val b = if (gotBase) base else x

        val preSuccOther = set.foldLeft((List[SOP](), List[SOP](), List[SOP]())){
          case ((pre, succ, other), n) => {
            if (checkIfSeq(n, b, identifySOPRelation(n, b, relations))) (n :: pre, succ, other)
            else if (checkIfSeq(b, n, identifySOPRelation(b, n, relations))) (pre, n :: succ, other)
            else (pre, succ, n :: other)
          }
        }

        def upd(x: List[SOP]) = x.map{
          case s: SOP if s.isEmpty => s
          case s: SOP => s.modify(sequencify(s.sop, relations, base))
        }

        val updPSO = (upd(groupThem(preSuccOther._1, relations)),
          upd(groupThem(preSuccOther._2, relations)),
          upd(groupThem(preSuccOther._3, relations)))

        Node(b, align(updPSO._1, rel, relations, base), align(updPSO._2, rel, relations, base), align(updPSO._3, rel, relations, base))
      }
    }
  }

  def checkIfSeq(pre: SOP, post: SOP, relation: SOP): Boolean = {
    (relation.isInstanceOf[Sequence] || relation.isInstanceOf[SometimeSequence]) &&
    relation.sop.toList == List(pre, post)
  }






  // fix sometime in seq
  def sopify(n: Node, rels: Map[Set[ID], SOP]): Seq[SOP] = {
    val sorted = sortNodes(n)

    sorted map {
      case x::Nil => x.s
      case seq @ x::xs => createSequenceSOP(seq, rels)
      case Nil => EmptySOP
    }
  }

  def sortNodes(n:Node): List[Seq[Node]] = {
    def order(n: Node): List[Node] = {
      n match {
        case m: Node if m == emptyNode => List[Node]()
        case Node(_,p,s,_) => order(p) ++ (n +: order(s))
      }
    }

    val aSeq = order(n)
    val otherSeq = aSeq.map(_.other) filter(_ != emptyNode)
    val moreSeq = for {
      n <- otherSeq
      s <- sortNodes(n) if s.nonEmpty
    } yield s

    moreSeq :+ aSeq
  }

  private def createSequenceSOP(seq: Seq[Node], rels: Map[Set[ID], SOP]): SOP = {
    def getSomtimeSeq(left: Seq[Node], prev: SOP, result: SOP): (SOP, Seq[Node]) = {
      left.toList match {
        case Nil => (result, left)
        case x::xs => {
          identifySOPRelation(x.s, prev, rels) match {
            case s: SometimeSequence => getSomtimeSeq(xs, x.s, result + x.s)
            case s: Sequence => (result, left)
          }
        }
      }
    }

    def getAlwaysSeq(left: Seq[Node], result: SOP): SOP = {
      left.toList match {
        case Nil => result
        case x::Nil => result + x.s
        case x::xs => {
          val rel = identifySOPRelation(x.s, xs.head.s, rels)
          // val rel = relations(Set(x.s, xs.head.s))
          rel match {
            case s: SometimeSequence => {
              val someTime = SometimeSequence(x.s)
              val pair = getSomtimeSeq(xs, x.s, someTime)
              getAlwaysSeq(pair._2, result + pair._1)
            }
            case s: Sequence => getAlwaysSeq(xs, result + x.s)
          }
        }
      }
    }

    val res = getAlwaysSeq(seq, Sequence())
    if (res.sop.size == 1) res.sop.head else res
  }




  def getOpsNames(s: SOP): String = {
    s match {
      case h: Hierarchy => h.operation.toString()
      case _ => s.sop.foldLeft("")(_ + "_--_--_" +getOpsNames(_))
    }
  }

  def opOpsName(ns: Seq[Node]) = {
    ns.foldLeft("")((res, node) => res + getOpsNames(node.s))
  }


}

