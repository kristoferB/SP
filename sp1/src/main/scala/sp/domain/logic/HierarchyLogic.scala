package sp.domain.logic

import sp.domain._

object HierarchyLogic extends HierarchyLogics

trait HierarchyLogics {
    implicit class HierarchyExtras(x: HierarchyRoot){
      def toIDAbleHierarchy(ids: List[IDAble]): IDAbleHierarchy = {
        val idMap = ids.map(i => i.id -> i).toMap
        def itr(node: HierarchyNode): Option[IDAbleHierarchy] = {
          val ch = node.children.flatMap(itr)
          idMap.get(node.item).map(IDAbleHierarchy(_, ch))
        }
        IDAbleHierarchy(x,x.children.flatMap(itr))
      }

      def getAllIDs: List[ID] = {
        getAllNodes.map(_.item)
      }

      def getChildren(id: ID): List[ID] = {
        getAllNodes.filter(_.item == id).flatMap(_.children.map(_.item))
      }

      def getParent(id: ID): Option[ID] = {
        getAllNodes.find(_.children.exists(_.item == id)).map(_.item)
      }

      def getNodes(f: HierarchyNode => Boolean) = {
        getAllNodes.filter(f)
      }

      def getAllNodes: List[HierarchyNode] = {
        def itr(node: HierarchyNode): List[HierarchyNode] = {
          node :: node.children.flatMap(itr)
        }
        x.children.flatMap(itr)
      }
    }


    implicit class IDAbleHierarchyExtras(x: IDAbleHierarchy){
      def toHierarchy: HierarchyRoot = {
        def itr(node: IDAbleHierarchy, oldCh: Map[ID, HierarchyNode]): HierarchyNode = {
          val ch = node.children.map(c => itr(c, oldCh))
          oldCh.getOrElse(node.item.id, HierarchyNode(node.item.id, List())).copy(children = ch)
        }
        val newH = x.item match {
          case hr: HierarchyRoot => hr
          case x => HierarchyRoot(x.name + "H")
        }

        val oldCh = newH.getAllNodes.map(x => x.item -> x).toMap
        val ch = x.children.map(c => itr(c, oldCh))
        newH.copy(children = ch)
      }

      def getAllItems: List[IDAble] = {
        getAllNodes.map(_.item)
      }

      def getChildren(id: ID): List[IDAble] = {
        getAllNodes.filter(_.item == id).flatMap(_.children.map(_.item))
      }

      def getParent(id: ID): Option[IDAble] = {
        getAllNodes.find(_.children.exists(_.item == id)).map(_.item)
      }

      def getNodes(f: IDAbleHierarchy => Boolean) = {
        getAllNodes.filter(f)
      }

      def getAllNodes: List[IDAbleHierarchy] = {
        def itr(node: IDAbleHierarchy): List[IDAbleHierarchy] = {
          node :: node.children.flatMap(itr)
        }
        x.children.flatMap(itr)
      }
    }



}
