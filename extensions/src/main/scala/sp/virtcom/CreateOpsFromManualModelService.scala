package sp.virtcom

import akka.actor._
import sp.domain.ID
import sp.jsonImporter.ServiceSupportTrait
import sp.system.messages._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import sp.domain.Logic._

/**
 * To create manufacturing operations and variables
 * from product operations
 *
 * To add a new service:
 * Register the service (actor) to SP (actor system) [sp.launch.SP]
 */
class CreateOpsFromManualModelService(modelHandler: ActorRef) extends Actor with ServiceSupportTrait {
  implicit val timeout = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(service, attr) => {

      println(s"service: $service")

      val id = attr.getAs[ID]("activeModelID").getOrElse(ID.newID)

      val psl = PSLFloorRoofCase()
      import CollectorModelImplicits._

      for {
        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))
        newIDables = psl.parseToIDables()
        _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id, modelVersion = modelInfo.version, items = newIDables.toList))
      } yield {
        newIDables.foreach(o => println(s"${o.name} a:${o.attributes.pretty}"))
      }

      sender ! "ok"

    }
  }

}

object CreateOpsFromManualModelService {
  def props(modelHandler: ActorRef) = Props(classOf[CreateOpsFromManualModelService], modelHandler)
}
