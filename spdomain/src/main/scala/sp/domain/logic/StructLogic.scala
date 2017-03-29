package sp.domain.logic

import sp.domain._

object StructLogic extends StructLogics

trait StructLogics {

  implicit def *(id: ID): StructNode = StructNode(id)
  implicit def *(item: IDAble): StructNode = StructNode(item.id)

  implicit class StructExtras(x: Struct) {
    def getChildren(node: StructNode): List[StructNode] = {
      x.items.filter(_.parent.contains(node.nodeID))
    }

    def getAllChildren(node: StructNode): List[StructNode] = {
      def rec(currentNode: StructNode, aggr: Set[StructNode]): Set[StructNode] = {
        if (aggr.contains(currentNode)) aggr
        else {
          val direct = getChildren(currentNode).toSet
          val updA = aggr ++ direct
          direct.flatMap(n => rec(n, updA))
        }
      }
      rec(node, Set()).toList
    }

    def getItemNodeMap: Map[ID, Set[StructNode]] = {
      x.items.foldLeft(Map[ID, Set[StructNode]]()) { (a, b) =>
        val m = a.getOrElse(b.item, Set()) + b
        a + (b.item -> m)
      }
    }


    def removeDuplicates(): Struct = {
      x.copy(items = x.items.distinct)
    }

    def hasLoops = {
      def req(currentNode: StructNode, aggr: Set[ID]): Boolean = {
        currentNode.parent match {
          case None => true
          case Some(n) if aggr.contains(n) => false
          case Some(n) =>
            val p = x.nodeMap(n)
            req(p, aggr + currentNode.nodeID)
        }
      }
      x.items.forall(s => req(s, Set()))
    }

    def <(node: StructNode) = {
      x.copy(items = x.items :+ node)
    }

    def <(xs: List[StructNode]) = {
      x.copy(items = x.items ++ xs)
    }



  }

  implicit class StructNodeForIdableExtras(x: IDAble) {
    val n = *(x)
  }

  implicit class StructNodeExtras(n: StructNode) {

  }

  // Use this mutual class to simplify the creation of a structure
//  class StructConstr(val node: StructNode, var p: Option[StructConstr], var ch: List[StructConstr]) {
//    def make: List[StructNode] = {
//      val xs = ch.flatMap(_.make)
//      val par = p.map(_.node.nodeID)
//      node.copy(parent = par)
//    }
//  }

}


