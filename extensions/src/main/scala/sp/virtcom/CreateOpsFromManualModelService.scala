package sp.virtcom

import akka.actor._
import sp.domain.{SPAttributes, ID}
import sp.jsonImporter.ServiceSupportTrait
import sp.system.messages._
import akka.pattern.ask
import akka.util.Timeout
import sp.virtcom.modeledCases.{PSLFloorRoofCase, VolvoWeldConveyerCase}
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
    case Request(service, attr, _, _) => {

      println(s"service: $service")

      lazy val activeModel = attr.getAs[SPAttributes]("activeModel")

      lazy val id = activeModel.flatMap(_.getAs[ID]("id")).getOrElse(ID.newID)

//            val manualModel = PSLFloorRoofCase()
      val manualModel = VolvoWeldConveyerCase()
      import CollectorModelImplicits._

      for {
        modelInfo <- futureWithErrorSupport[ModelInfo](modelHandler ? GetModelInfo(id))
        newIDables = manualModel.parseToIDables()
      infoText = s"Model added from \'${manualModel.getClass.getSimpleName}\'"
        _ <- futureWithErrorSupport[Any](modelHandler ? UpdateIDs(model = id, items = newIDables.toList, info = SPAttributes("info"->infoText)))
      } yield {
        //        newIDables.foreach(o => println(s"${o.name} a:${o.attributes.pretty}"))
      }

      sender ! "ok"

    }
  }

}

object CreateOpsFromManualModelService {
  def props(modelHandler: ActorRef) = Props(classOf[CreateOpsFromManualModelService], modelHandler)
}
