package sp.services.sopmaker

import akka.actor._
import sp.domain._

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
case class MakeASOP(ops: List[ID], relations: RelationMap)

class SOPMaker extends Actor with Groupify {
  def receive = {
    case MakeASOP(ops, rels) => {

    }
  }
}

//TODO: Move these to domain.logic. SOP logic
trait Groupify {


  def makeSOPsFromOpsID(ops: List[ID]): List[SOP] = ops map SOP.apply


  /**
   * Identifies the relation between two SOPs. Requires that all children have relation in the map
   * @param sop1
   * @param sop2
   * @param relations The SOP relation map containing relation among children
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
      else relations(Set(s1, s2)).modify(List())
    }) toSet

    if (relationBetweenPairs.size == 1) relationBetweenPairs.head.modify(List(sop1, sop2))
    else if (relationBetweenPairs == Set(Sequence(), SometimeSequence())) Sequence(sop1, sop2)
    else Other(sop1, sop2)

  }

  def extractOps(sop: SOP): List[ID] = {
    sop match {
      case x: Hierarchy => List(x.operation) // does not need to digg since the op is included in the relationMap
      case x: SOP => x.children flatMap extractOps toList
    }
  }

  /**
   * Takes a list of newly created groups and identifies the relation among them
   * based on the relation of the children
   * @param sops the list od groups
   * @param relations the current relation map
   * @return an Updated relation map
   */
  def updateSOPRelationMap(sops: List[SOP], relations: Map[Set[SOP], SOP]) = {
    val createdRelations = for {
      s1 <- sops
      s2 <- sops if s1 != s2
      pairRelation <- relations.get(Set(s1, s2))
    } yield Set(s1, s2) -> pairRelation
    relations ++ createdRelations.toMap
  }


  def groupify(sopsToGroup: List[SOP],
               relations: Map[Set[ID], SOP],
               relationToGroup: SOP => Boolean,
               createSOP: List[SOP] => SOP): List[SOP] = {

    val sops = sopsToGroup map { sop => if (sop.children.isEmpty) sop else sop.modify(groupify(sop.children.toList, relations, relationToGroup, createSOP))}

    val relatedPairs = for {
      x <- sops
      y <- sops if x!=y
      rel <- Some(identifySOPRelation(x, y, relations)) if relationToGroup(rel)
    } yield Set(x, y)

    def mergeTheGroups(theGroups: Set[Set[SOP]]): Set[Set[SOP]] = {
      val merge = theGroups.foldLeft(Set[Set[SOP]]())({
        (b, a) => {
          val filter = b partition (!_.intersect(a).isEmpty)
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

    val newGroups = mergeIntoGroups map (set => createSOP(set.toList)) toList

    newGroups ++ sopsNotAddedToGroup

  }


}

trait Sequencify {

}
