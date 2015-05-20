package sp.virtcom

import akka.actor._
import sp.system.messages._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * To create manufacturing operations and variables
 * from product operations
 *
 * To add a new service:
 * Register the service (actor) to SP (actor system) [sp.launch.SP]
 * Make service selectable from gui [web/app/scripts/controllers/serviceMeny.js]
 */
class CreateManufOpsFromProdOpsService(modelHandler: ActorRef) extends Actor {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(_, attr) => {

      for {
      //Get all models in SP
        ModelInfos(modelInfoList) <- futureWithErrorSupport[ModelInfos](modelHandler ? GetModels)

      } yield {
        println(s"Models in SP: ${modelInfoList.map(_.name).mkString("\n")} $attr")
      }

    }

  }

  def futureWithErrorSupport[T](f: Future[Any]): Future[T] =
    for {
      obj <- f
    } yield {
      if (obj.isInstanceOf[SPError]) println(s"Error $obj")
      obj.asInstanceOf[T]
    }

}

object CreateManufOpsFromProdOpsService {
  def props(modelHandler: ActorRef) = Props(classOf[CreateManufOpsFromProdOpsService], modelHandler)
}
