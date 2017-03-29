package sp.virtcom

import akka.actor._
import sp.domain.{ID, SPValue, HierarchyRoot, SPAttributes}
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

object MakeModelService extends SPService {
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
    TransformValue("setup", _.getAs[MakeModelSetup]("setup"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)

  // important to incl ServiceLauncher if you want unique actors per request
  def props(sh:ActorRef) = ServiceLauncher.props(Props(classOf[MakeModelService], sh))

}

case class MakeModelSetup(model: String)

class MakeModelService(sh: ActorRef) extends Actor with ServiceSupport with AddHierarchies {
  import scala.concurrent.duration._
  import akka.util.Timeout
  implicit val timeout = Timeout(100.seconds)
  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) =>

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      println(s"service: $service")

      implicit val hierarchyRoots = filterHierarchyRoots(ids)

      val selectedModel = transform(MakeModelService.transformTuple)

      val modelMap = MakeModelService.models.map(m => m.modelName -> m).toMap


      val manualModel = modelMap.getOrElse(selectedModel.model, TrucksCase())

      import CollectorModelImplicits._

      val idablesFromModel = manualModel.parseToIDables()
      val idablesToReturn = idablesFromModel ++ addHierarchies(idablesFromModel, "hierarchy")

      for {
        Response(ids,_,_,_) <- askAService(Request("ExtendIDablesBasedOnAttributes",
          SPAttributes("core" -> ServiceHandlerAttributes(model = None,
            responseToModel = false,onlyResponse = true, includeIDAbles = List())),
          idablesToReturn, ID.newID), sh)

        ids_merged = idablesToReturn.filter(x=> !ids.exists(y=>y.id==x.id)) ++ ids

        Response(ids2,synthAttr,_,_) <- askAService(Request("SynthesizeModelBasedOnAttributes",
          SPAttributes("core" -> ServiceHandlerAttributes(model = None,
            responseToModel = false, onlyResponse = true, includeIDAbles = List())),
          ids_merged, ID.newID), sh)

        ids_merged2 = ids_merged.filter(x=> !ids2.exists(y=>y.id==x.id)) ++ ids2

      } yield {
        rnr.reply ! Response(ids_merged2, SPAttributes("info" -> s"Model created from: ${manualModel.modelName}"), service, reqID)
        progress ! PoisonPill
        self ! PoisonPill
      }



    case (r: Response, reply: ActorRef) =>
      reply ! r
    case x =>
      sender() ! SPError("What do you whant me to do? " + x)
      self ! PoisonPill

  }

}
