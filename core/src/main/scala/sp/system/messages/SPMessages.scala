package sp.system.messages

import sp.domain._

trait SPCommand
trait SPEvent


case class SPOK(isa: String = "SPOK") extends SPEvent


// Error messages
trait SPError extends SPEvent
object SPError {
  def apply(s: String): SPError = SPErrorString(s)
  def apply(xs: List[SPError]): SPError = SPErrors(xs)
}
case class SPErrorString(error: String) extends SPError
case class SPErrors(errors: List[SPError]) extends SPError
case class UpdateError(currentModelVersion: Long, conflicts: List[ID]) extends SPError
case class MissingID(id: ID, error: String = s"The Model does not contain that id") extends SPError
case class ServiceError(service: String, id: ID, serviceError: SPError) extends SPError