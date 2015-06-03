package sp.virtcom

import akka.actor._
import sp.domain.{Operation, ID}
import sp.jsonImporter.ServiceSupportTrait
import sp.system.messages._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

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

      val id = attr.getAsID("activeModelID").getOrElse(ID.newID)

      val psl = PSLFloorRoofCase()

      import SupervisorImplicits._
      psl.createModel("./testFiles/gitIgnore/")

//      val result = for {
//        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))
//        newOps = List(Operation("IamTheNewOp"))
//        _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id, modelVersion = modelInfo.version, items = newOps))
//
//      } yield {
//          "ok"
//        }

      sender ! "ok"

    }
  }

}

object CreateOpsFromManualModelService {
  def props(modelHandler: ActorRef) = Props(classOf[CreateOpsFromManualModelService], modelHandler)
}
