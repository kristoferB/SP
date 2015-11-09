package sp.virtcom

import akka.actor._
import sp.domain.{HierarchyRoot, HierarchyNode, IDAble, SPAttributes}
import sp.system._
import sp.system.{ServiceLauncher, SPService}
import sp.system.messages._
import sp.virtcom.modeledCases.{GKNcase, ROARcase, PSLFloorRoofCase, VolvoWeldConveyerCase}
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
      "model" -> KeyDefinition("String", List(VolvoWeldConveyerCase().modelName, GKNcase().modelName, ROARcase().modelName, PSLFloorRoofCase().modelName), Some(GKNcase().modelName))
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

      val selectedModel = transform(CreateOpsFromManualModelService.transformTuple)

      var manualModel: CollectorModel = GKNcase()
      if (selectedModel.model.equals(PSLFloorRoofCase().modelName)) {
        manualModel = PSLFloorRoofCase()
      } else if (selectedModel.model.equals(ROARcase().modelName)) {
        manualModel = ROARcase()
      } else if (selectedModel.model.equals(VolvoWeldConveyerCase().modelName)) {
        manualModel = VolvoWeldConveyerCase()
      }

      import CollectorModelImplicits._

      val idablesFromModel = manualModel.parseToIDables()
      val idablesToReturn = idablesFromModel ++ addHierarchies(idablesFromModel, "hierarchy")

      rnr.reply ! Response(idablesToReturn, SPAttributes("info" -> s"Model created from: ${VolvoWeldConveyerCase().modelName}"), service, reqID)
      progress ! PoisonPill
      self ! PoisonPill

    case (r: Response, reply: ActorRef) =>
      reply ! r
    case x =>
      sender() ! SPError("What do you whant me to do? " + x)
      self ! PoisonPill

  }

}

trait AddHierarchies {
  def addHierarchies(idables: List[IDAble], attributeKey: String): List[IDAble] = {
    val hierarchyMap = idables.foldLeft(Map(): Map[String, List[HierarchyNode]]) { case (acc, idable) =>
      idable.attributes.getAs[Set[String]](attributeKey) match {
        case Some(hierarchies) =>
          acc ++ hierarchies.map { hierarchy =>
            hierarchy -> (HierarchyNode(idable.id) +: acc.getOrElse(hierarchy, List()))
          }
        case _ => acc
      }
    }
    hierarchyMap.map { case (hierarchy, nodes) =>
      HierarchyRoot(hierarchy, nodes)
    }.toList
  }
}