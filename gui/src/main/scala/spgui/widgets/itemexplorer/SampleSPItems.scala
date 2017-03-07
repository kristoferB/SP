package spgui.widgets.itemexplorer

import sp.domain._

object SampleSPItems {
  val gustaf = Operation("Gustaf")
  val klas = Operation("Klas")
  val rot = HierarchyRoot(
    "Rot",
    HierarchyNode(gustaf.id) ::
      HierarchyNode(klas.id) ::
      Nil
    )
  val wilhelm = Operation("Wilhelm")

  val sampleSPItems = rot :: wilhelm :: Nil

  def apply() = sampleSPItems
}


// everything below here I just pasted in to be able to create sample data
// it doesn't belong here
// TODO: move to spdomain (guis?) or wherever it should be

case class Operation(name: String,
                     conditions: List[Condition] = List(),
                     attributes: SPAttributes = SPAttributes(),
                     id: ID = ID.newID)
    extends IDAble { }


case class HierarchyRoot(name: String,
                         children: List[HierarchyNode] = List(),
                         attributes: SPAttributes = SPAttributes(),
                         id: ID = ID.newID) extends Specification

case class HierarchyNode(item: ID, children: List[HierarchyNode] = List(), id: ID = ID.newID)


trait IDAble {
  val name: String
  val id: ID
  val attributes: SPAttributes

  def |=(x: Any) = x match {
    case m: IDAble => m.id.equals(id)
    case _ => false
  }
}


trait Specification extends IDAble {
  val name: String
}


case class ID(value: java.util.UUID){
  override def toString = value.toString
}


trait Condition {
  val attributes: SPAttributes
}


object ID {
  implicit def uuidToID(id: java.util.UUID) = ID(id)
  implicit def idToUUID(id: ID) = id.value
  def newID = ID(java.util.UUID.randomUUID())
  def makeID(id: String): Option[ID] = {
    try {
      Some(ID(java.util.UUID.fromString(id)))
    } catch {
      case e: IllegalArgumentException => None
    }
  }
  def isID(str: String) = {
    makeID(str) != None
  }
}
