package sp.system.messages

import sp.domain._
import spray.http.{StatusCodes, StatusCode}

trait SPMessage

// Error messages
trait SPError extends SPMessage
object SPError {
  def apply(s: String): SPError = SPErrorString(s)
  def apply(c: StatusCode, s: String): SPError = SPErrorCodeAndString(c, s)
}
case class SPErrorString(error: String) extends SPError
case class SPErrorCodeAndString(code: StatusCode, error: String) extends SPError
case class UpdateError(currentModelVersion: Long, conflicts: List[ID]) extends SPError
case class MissingID(id: ID, model: ID,  error: String = s"The Model does not contain that id")