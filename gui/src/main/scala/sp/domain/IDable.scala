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
  val id: UUID
  val attributes: SPAttributes

  def |=(x: Any) = x match {
    case m: IDAble => m.id.equals(id)
    case _ => false
  }
}
