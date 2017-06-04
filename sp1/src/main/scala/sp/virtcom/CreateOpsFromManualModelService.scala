package sp.virtcom

import akka.actor._
import sp.domain.{SPValue, HierarchyRoot, SPAttributes}
import sp.services.AddHierarchies
import sp.system._
import sp.system.{ServiceLauncher, SPService}
import sp.system.messages._
import sp.virtcom.modeledCases._
import sp.domain.Logic._

/**
 * To create manufacturing operations and variables
 * from product operations
 *
 * To add a new service:
 * Register the service (actor) to SP (actor system) [sp.launch.SP]
 */

object CreateOpsFromManualModelService extends SPService {
  val models = List(
    VolvoWeldConveyerCase(),
    GKNcase(),
    GKNSmallcase(),
    ROARcase(),
    PSLFloorRoofCase(),
    TrucksCase()
  )

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Creates operations for a manual model"
    ),
    "setup" -> SPAttributes(
      "model" -> KeyDefinition("String", models.map(m => SPValue(m.modelName)), Some(TrucksCase().modelName))
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

class CreateOpsFromManualModelService extends Actor with ServiceSupport with AddHierarchies {

  def receive = {
    case r@Request(service, attr, ids, reqID) =>

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      println(s"service: $service")

      implicit val hierarchyRoots = filterHierarchyRoots(ids)

      val selectedModel = transform(CreateOpsFromManualModelService.transformTuple)

      val modelMap = CreateOpsFromManualModelService.models.map(m => m.modelName -> m).toMap


      val manualModel = modelMap.getOrElse(selectedModel.model, GKNcase())

      import CollectorModelImplicits._

      val idablesFromModel = manualModel.parseToIDables()
      val idablesToReturn = idablesFromModel ++ addHierarchies(idablesFromModel, "hierarchy")

      rnr.reply ! Response(idablesToReturn, SPAttributes("info" -> s"Model created from: ${manualModel.modelName}"), service, reqID)
      progress ! PoisonPill
      self ! PoisonPill

    case (r: Response, reply: ActorRef) =>
      reply ! r
    case x =>
      sender() ! SPError("What do you whant me to do? " + x)
      self ! PoisonPill

  }

}
