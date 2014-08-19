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
case class MakeASOP(ops: List[Operation], relation: Map[Set[SOP], SOP])

class SOPMaker extends Actor with Groupify {
  def receive = {
    case MakeASOP(ops, svs, init) => {

    }
  }
}

//TODO: Move these to domain.logic. SOP logic
trait Groupify {


  def makeSOPsFromOpsID(ops: List[SOP]) = ops map Hierarchy
  

  trait groupSOPs {
    def groupType: SOP => Boolean
    def createGroup: Set[SOP] => SOP

    def groupify(sop: SOP, relations: Map[Set[SOP], SOP]): SOP = {
      def groupThem(sop: SOP): Set[Set[SOP]] = {
        val relatedPairs = for {
          x <- sop.children
          y <- sop.children if x != y && groupType(relations(Set(x,y)))
        } yield (x, y)

        val groupMap = relatedPairs.foldLeft(Map[SOP, Set[SOP]]())((b, a) => {
          if (b contains a._1) {
            b + (a._1 -> (b(a._1) + a._2))
          } else b + (a._1 -> Set(a._2))
        })

        (groupMap map (t => t._2 + t._1)) toSet
      }

      def groupUnifyier(gs: Set[Set[SOP]]): Set[Set[SOP]] = {
        val i = gs.foldLeft(Set[Set[SOP]]())({
          (b, a) => {
            val filter = b partition(!_.intersect(a).isEmpty)
            val union = a ++ filter._1.foldLeft(Set[SOP]())(_ ++ _)
            filter._2 + union
          }
        })

        if (i != gs) groupUnifyier(i)
        else i
      }


      val groupedChildren = sop.modify(for {
        s <- sop.children
      } yield { if (s.isEmpty) s else groupify(s, relations) })
      val groups = groupThem(groupedChildren)
      val unifiedGroups = groupUnifyier(groups)
      val result = unifiedGroups.map(createGroup)
      val nonGrouped = groupedChildren.children filter (s => !(unifiedGroups exists (_ contains s)))
      sop modify ((result.toList ++ nonGrouped))
    }
  }
}

trait Sequencify {

}
