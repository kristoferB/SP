package sp.domain

/**
 * Created by Kristofer on 2014-06-12.
 */
trait Specification extends IDAble {
  val name: String
}

case class SOPSpec(name: String,
                   sop: List[SOP],
                   attributes: SPAttributes = SPAttributes(),
                   id: ID = ID.newID) extends Specification

case class SPSpec(name: String,
                  attributes: SPAttributes = SPAttributes(),
                  id: ID = ID.newID) extends Specification

// To be used for defining hierarchies in SP
case class HierarchyRoot(name: String,
                         children: List[HierarchyNode] = List(),
                         attributes: SPAttributes = SPAttributes(),
                         id: ID = ID.newID) extends Specification

case class HierarchyNode(item: ID, children: List[HierarchyNode] = List(), id: ID = ID.newID)

case class IDAbleHierarchy(item: IDAble, children: List[IDAbleHierarchy])


/**
  * A flat structure of items, replacing Hierarchy
  * @param name The name of the structure
  * @param items
  * @param attributes
  * @param id
  */
case class Struct(name: String,
                  items: List[StructNode] = List(),
                  attributes: SPAttributes = SPAttributes(),
                  id: ID = ID.newID) extends Specification

case class StructNode(item: ID, parent: Option[ID] = None, nodeID: ID = ID.newID)


