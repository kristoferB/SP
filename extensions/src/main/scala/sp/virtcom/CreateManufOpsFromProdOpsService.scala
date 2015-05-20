package sp.virtcom

import akka.actor._
import sp.domain.Operation
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
 */
class CreateManufOpsFromProdOpsService(modelHandler: ActorRef) extends Actor {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(service, attr) => {
      println(s"service: $service")

      for {
        id <- attr.getAsID("activeModelID")
//        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))
        newOps = List(Operation("IamTheNewOp"))
//        _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id, modelVersion = modelInfo.version, items = newOps))

      } yield {
        sender ! "done"
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
