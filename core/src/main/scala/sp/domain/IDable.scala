package sp.domain

import java.util.UUID

/**
 * All things used in the domain should be an IDAble. When a new object is created, a new random ID is created.
 *
 *
 * When an object is updated, the model handler will reuse the id and increment the version.
 * The plan is that only the model handler should do this.
 *
 * Created by Kristofer on 2014-06-07.
 */
trait IDAble {
  /**
   * IDAbles need to implement this. When updating an IDAble, first create a new one
   * e.g. val op = Operations("o1", cond, attr) or based on op.copy. The new object
   * will have a new id and version -1. The model will then update the object
   * with the correct ID and an incremented version, after it has verified the update
   * @param currentID - The ID the updated IDAble have
   * @param currentVersion - The version the updated IDAble have
   */
  def update(currentID: ID, currentVersion: Long): IDAble

  lazy val id: ID = ID(UUID.randomUUID())
  lazy val version: Long = -1
  val attributes: SPAttributes

  def |=(x: Any) = x match {
    case m: IDAble => m.id.equals(id)
    case _ => false
  }
  def <(x: IDAble) = x.id.equals(id) && version < x.version

  override def hashCode = {
    val prime = 31;
    prime*(prime + id.hashCode()) + version.hashCode()
  }

  override def equals(obj: Any) = obj match {
    case x: IDAble => x.id == id && x.version == version
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
