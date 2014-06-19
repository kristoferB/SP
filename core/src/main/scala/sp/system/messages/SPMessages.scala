package sp.system.messages

import sp.domain._

trait SPMessage

// Error messages
trait SPError extends SPMessage
object SPError {
  def apply(s: String): SPError = SPErrorString(s)
}
case class SPErrorString(error: String) extends SPError
case class UpdateError(yourModelVersion: Long, currentModelVersion: Long, conflicts: List[ID]) extends SPError