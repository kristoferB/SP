package sp.domain

import java.util.UUID
import sp.domain._

sealed trait IDAble {
  val name: String
  val id: UUID
  val attributes: SPAttributes

  def |=(x: Any) = x match {
    case m: IDAble => m.id.equals(id)
    case _ => false
  }
}

case class Operation(name: String,
                     conditions: List[Condition] = List(),
                     attributes: SPAttributes = SPAttributes(),
                     id: ID = ID.newID) extends IDAble

case class Thing(name: String,
                 attributes: SPAttributes = SPAttributes(),
                 id: ID = ID.newID) extends IDAble


case class SOPSpec(name: String,
                   sop: List[SOP],
                   attributes: SPAttributes = SPAttributes(),
                   id: ID = ID.newID) extends IDAble


case class SPSpec(name: String,
                  attributes: SPAttributes = SPAttributes(),
                  id: ID = ID.newID) extends IDAble

case class SPResult(name: String,
                  attributes: SPAttributes = SPAttributes(),
                  id: ID = ID.newID) extends IDAble

case class SPState(name: String = "state",
                   state: Map[ID, SPValue],
                   attributes: SPAttributes = SPAttributes(),
                   id: ID = ID.newID) extends IDAble

case class Struct(name: String,
                  items: List[StructNode] = List(),
                  attributes: SPAttributes = SPAttributes(),
                  id: ID = ID.newID) extends IDAble {
  lazy val nodeMap = items.map(l => l.nodeID -> l).toMap
}

case class StructNode(item: ID,
                      parent: Option[ID] = None,
                      nodeID: ID = ID.newID,
                      attributes: SPAttributes = SPAttributes())




