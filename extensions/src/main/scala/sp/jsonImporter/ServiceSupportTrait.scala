package sp.jsonImporter

import sp.system.messages.SPError
import scala.concurrent.Future

trait ServiceSupportTrait {
  import scala.concurrent.ExecutionContext.Implicits.global
  def futureWithErrorSupport[T](f: Future[Any]): Future[T] =
    for {
      obj <- f
    } yield {
      if (obj.isInstanceOf[SPError]) println(s"Error $obj")
      obj.asInstanceOf[T]
    }
}
