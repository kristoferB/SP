package sp.services

import sp.domain.{HierarchyRoot, HierarchyNode, IDAble}
import sp.domain.Logic._

/**
 * Method to loop given list of idables and
 * 1) Create HierarchyRoots based on what the idables attributes
 * 2) Create a HierarchyNode for each idable pointing to a specific node.
 * Created by patrik on 2015-11-27.
 */
trait AddHierarchies {

  def filterHierarchyRoots(ids : List[IDAble]) = ids.filter(_.isInstanceOf[HierarchyRoot]).map(_.asInstanceOf[HierarchyRoot])

  /**
   * attributeKey is the attribute key to look for.
   * idables is the list of idables looped.
   * For each idable with an attributeKey match an HierarchyNode is created in the appropriate HierarchyRoot
   * For example with an attributeKey "aK" the an idable with containing an attribute "ak" -> Set("A","B")
   * will get one HierarchyNode in the HierarchyRoot A and one in B.
   * The HierarchyRoot are created if they does not exists.
   */
  def addHierarchies(idables: List[IDAble], attributeKey: String)(implicit existingHierarchyRoots: List[HierarchyRoot] = List()): List[IDAble] = {
    val hierarchyMap = idables.foldLeft(Map(): Map[String, List[HierarchyNode]]) { case (acc, idable) =>
      idable.attributes.getAs[Set[String]](attributeKey) match {
        case Some(hierarchies) =>
          acc ++ hierarchies.map { hierarchy =>
            hierarchy -> (HierarchyNode(idable.id) +: acc.getOrElse(hierarchy, List()))
          }
        case _ => acc
      }
    }
    hierarchyMap.map { case (hierarchy, nodes) =>
      val a = existingHierarchyRoots.map(hr => hr.name -> hr).toMap
      val b = a.getOrElse(hierarchy, HierarchyRoot(hierarchy))
      b.copy(children = b.children ++ nodes)
    }.toList
  }
}
