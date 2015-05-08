package sp.domain

import java.util.UUID

/**
 *
 * All things used in the domain should be an IDAble. When a new object is created, a new random ID is created.
 *
 * When an object is updated, the model handler will reuse the id and increment the version.
 * The plan is that only the model handler should do this.
 *
 * Created by Kristofer on 2014-06-07.
 */
trait IDAble {
  val name: String
  val id: ID
  val attributes: SPAttributes

  def |=(x: Any) = x match {
    case m: IDAble => m.id.equals(id)
    case _ => false
  }
}


case class ID(value: UUID){
  override def toString() = value.toString
}

object ID {
  implicit def uuidToID(id: UUID) = ID(id)
  implicit def idToUUID(id: ID) = id.value
  def newID = ID(UUID.randomUUID())
  def makeID(id: String): Option[ID] = {
    try {
      Some(ID(UUID.fromString(id)))
    } catch {
      case e: IllegalArgumentException => None
    }
  }
}
