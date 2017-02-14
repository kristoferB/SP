package sp.virtcom

import akka.actor.{PoisonPill, Props, Actor, ActorRef}
import sp.domain._
import sp.services.AddHierarchies
import sp.system.{ServiceSupport, ServiceLauncher, SPService}
import sp.system.messages._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._

/**
 * 
 * Run the following services in sequence => CreateInstance... -> Synth... -> RelationIdent
 * Rename the result to match the input SOP.
 *
 */

object CreateParallelInstanceService extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Create parallel instance(s) form type model"
    ),
    "Specifications" -> SPAttributes(
      "sops" -> KeyDefinition("List[ID]", List(), Some(SPValue(List()))),
      "iterations" -> KeyDefinition("Int", List(100, 400, 1000, 2000), Some(400))
    )
  )

  val transformTuple = (
    TransformValue("Specifications", _.getAs[CreateParallelInstanceSpecifications]("Specifications"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(serviceHandler : ActorRef) =
    ServiceLauncher.props(Props(classOf[CreateParallelInstanceService], serviceHandler))

}

case class CreateParallelInstanceSpecifications(sops: List[ID], iterations : Int)

class CreateParallelInstanceService(serviceHandler : ActorRef) extends Actor with ServiceSupport with AddHierarchies {
  implicit val timeout = Timeout(600 seconds)
  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) =>

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val progress = context.actorOf(progressHandler)

      println(s"service: $service")

      implicit val hierarchyRoots = filterHierarchyRoots(ids)

      val specifications = transform(CreateParallelInstanceService.transformTuple)
      val sopSpecs = ids.filter(_.isInstanceOf[SOPSpec]).map(_.asInstanceOf[SOPSpec]).filter(sop => specifications.sops.contains(sop.id))

      progress ! SPAttributes("progress" -> "CreateParallelInstanceService started")

      val instance_attrs = SPAttributes(
          "Specifications" -> SPAttributes("sops" -> specifications.sops,"generateIdables" -> true),
          "core" -> ServiceHandlerAttributes(model = None,responseToModel = false,onlyResponse = true, includeIDAbles = List()))
      
      for {
        Response(ids,_,_,_) <- askAService(
          Request("CreateInstanceModelFromTypeModel", instance_attrs, ids, ID.newID), serviceHandler)

        synth_attrs = SPAttributes(
          "core" -> ServiceHandlerAttributes(
            model = None,responseToModel = false,onlyResponse = true, includeIDAbles = List()))

        Response(ids2,_,_,_) <- askAService(
          Request("SynthesizeModelBasedOnAttributes", synth_attrs, ids, ID.newID), serviceHandler)

        // ids2 contains updated operations. merge with ids (all items in hierachy)
        ids3 = ids.filter(!_.isInstanceOf[Operation]) ++ ids2

        relation_attrs = SPAttributes(
          "setup" -> SPAttributes(
            "iterations" -> specifications.iterations,
            "operationIds" -> List()),
          "core" -> ServiceHandlerAttributes(
            model = None,responseToModel = false,onlyResponse = true, includeIDAbles = List()))

        Response(resultSOP,_,_,_) <- askAService(
          Request("RelationIdentification", relation_attrs, ids3, ID.newID), serviceHandler)

      } yield {
        println("INSTANCE: " + ids.length)
        println("SYNTH: " + ids2.length)
        println("COMBINED: " + ids3.length)
        println("RESULTING SOP: " + resultSOP.length)

        if(resultSOP.isEmpty) {
          rnr.reply ! Response(List(), SPAttributes(), service, reqID)
          progress ! PoisonPill
          self ! PoisonPill
        } else {
          // rename resulting sop and keep only the first sequence
          val r = resultSOP(0).asInstanceOf[SOPSpec]
          val rl = List(r.copy(name = sopSpecs(0).name + "_Result", sop = r.sop.take(1), attributes = SPAttributes("hierarchy" -> Set("Results"))))

          // only send back operations
          val ops = ids3.filter(_.isInstanceOf[Operation])
          // need to fix the hierarchy otherwise end up with invalid children
          val root = ids3.filter(_.isInstanceOf[HierarchyRoot]).map(_.asInstanceOf[HierarchyRoot])
          val nr = root.map(r => r.copy(children = r.children.filter(x => ops.exists(_.id == x.item))))
          rnr.reply ! Response(nr ++ ops ++ rl ++ addHierarchies(rl, "hierarchy"),
            SPAttributes("info" -> s"created a parallel instance for ${sopSpecs(0).name}"),
            service, reqID)
          progress ! PoisonPill
          self ! PoisonPill
        }
      }

    case (r: Response, reply: ActorRef) =>
      reply ! r
    case x =>
      sender() ! SPError("What do you whant me to do? " + x)
      self ! PoisonPill
  }
}
