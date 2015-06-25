package sp.system.messages

import sp.domain._

trait SPMessage

// Error messages
trait SPError extends SPMessage
object SPError {
  def apply(s: String): SPError = SPErrorString(s)
  def apply(xs: List[String]): SPError = SPErrors(xs)
}
case class SPErrorString(error: String) extends SPError
case class SPErrors(errors: List[String]) extends SPError
case class UpdateError(currentModelVersion: Long, conflicts: List[ID]) extends SPError
case class MissingID(id: ID, model: ID,  error: String = s"The Model does not contain that id")