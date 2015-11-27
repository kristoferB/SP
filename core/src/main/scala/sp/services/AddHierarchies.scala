package sp.services

import sp.domain.{HierarchyRoot, HierarchyNode, IDAble}
import sp.domain.Logic._

/**
 * Method to loop given list of idables and
 * 1) Create HierarchyRoots based on what the idables attributes
 * 2) Create a HierarchyNode for each idable pointing to a specific node.
 *
 * TODO Not create new HierarchyRoots for existing HierarchyRoots...
 *
 * Created by patrik on 2015-11-27.
 */
trait AddHierarchies {
  def addHierarchies(idables: List[IDAble], attributeKey: String): List[IDAble] = {
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
      HierarchyRoot(hierarchy, nodes)
    }.toList
  }
}
