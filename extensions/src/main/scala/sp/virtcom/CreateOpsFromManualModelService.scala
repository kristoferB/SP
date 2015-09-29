package sp.virtcom

import akka.actor._
import sp.domain.SPAttributes
import sp.system._
import sp.system.{ServiceLauncher, SPService}
import sp.system.messages._
import sp.virtcom.modeledCases.{PSLFloorRoofCase, VolvoWeldConveyerCase}
import sp.domain.Logic._

/**
 * To create manufacturing operations and variables
 * from product operations
 *
 * To add a new service:
 * Register the service (actor) to SP (actor system) [sp.launch.SP]
 */

object CreateOpsFromManualModelService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Creates operations for a manual model"
    ),
    "setup" -> SPAttributes(
      "model" -> KeyDefinition("String", List("Volvo Weld Conveyer","PSL - floor roof"), Some("Volvo Weld Conveyer"))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[CreateOpsFromManualModelSetup]("setup"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)

  // important to incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[CreateOpsFromManualModelService]))

}

case class CreateOpsFromManualModelSetup(model: String)

class CreateOpsFromManualModelService extends Actor with ServiceSupport {

  def receive = {
    case r@Request(service, attr, ids, reqID) =>

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      println(s"service: $service")

      val selectedModel = transform(CreateOpsFromManualModelService.transformTuple)

      var manualModel :CollectorModel = VolvoWeldConveyerCase()
      if (selectedModel.model.equals("PSL - floor roof")) {
        manualModel = PSLFloorRoofCase()
      }


      import CollectorModelImplicits._

      rnr.reply ! Response(manualModel.parseToIDables(), SPAttributes(), service, reqID)
      progress ! PoisonPill
      self ! PoisonPill

    case (r: Response, reply: ActorRef) => {
      reply ! r
    }
    case x => {
      sender() ! SPError("What do you whant me to do? " + x)
      self ! PoisonPill
    }

  }

}