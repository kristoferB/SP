package sp.services.relations

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._

object RelationIdentification extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Find relations" // to organize in gui. maybe use "hide" to hide service in gui
      ),
    "setup" -> SPAttributes())

  val transformTuple = (
    TransformValue("setup", _.getAs[RelationIdentification]("setup")))

  val transformation = transformToList(transformTuple.productIterator.toList)

  // important to incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[RelationIdentification]))

  // Alla får även "core" -> ServiceHandlerAttributes

  //  case class ServiceHandlerAttributes(model: Option[ID],
  //                                      responseToModel: Boolean,
  //                                      onlyResponse: Boolean,
  //                                      includeIDAbles: List[ID])

}

case class OpState(op: ID, state: State)
case class OperationRelation(op1: OpState, op2: OpState, sop: SOP)

case class RelationIdentificationSetup(onlyOperations: Boolean, searchMethod: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class RelationIdentification extends Actor with ServiceSupport with RelationIdentificationLogic {
  import context.dispatcher

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you whant to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      println(s"I got: $r")

      //val s = transform(RelationMapper.transformTuple._1)

      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get
      println(s"core är även med: $core")

      val ops = ids.filter(item => item.isInstanceOf[Operation])

      def mapOps[T](t: T) = ops.map(o => o.id -> t).toMap
      val iState = State(mapOps(OperationState.init))
      val evalualteProp2 = EvaluateProp(mapOps((_: SPValue) => true), Set(), TwoStateDefinition)

      //buildRelationMap(createItemRelationsfrom(findStraightSeq(ops, iState, evalualteProp2))) 

    }
    case (r: Response, reply: ActorRef) => {
      reply ! r
    }
    case ("boom", replyTo: ActorRef) => {
      replyTo ! SPError("BOOM")
      self ! PoisonPill
    }
    case x => {
      sender() ! SPError("What do you whant me to do? " + x)
      self ! PoisonPill
    }
  }

  import scala.concurrent._
  import scala.concurrent.duration._
  def sendResp(r: Response, progress: ActorRef)(implicit rnr: RequestNReply) = {
    context.system.scheduler.scheduleOnce(2000 milliseconds, self, (r, rnr.reply))
    //context.system.scheduler.scheduleOnce(1000 milliseconds, self, ("boom", rnr.reply))

    //    rnr.reply ! r
    //    progress ! PoisonPill
    //    self ! PoisonPill
  }

}
case class ItemRelation(id: ID, state: SPValue, relations: Map[ID, Set[SPValue]])
case class OperationRelation(opPair: Set[Operation], relation: Set[SOP]) //Pair is a tuple

sealed trait RelationIdentificationLogic {
  def findStraightSeq(ops: List[Operation], initState: State, evalSetup: EvaluateProp): Seq[Operation] = {
    implicit val es = evalSetup

    def getEnabledOperations(state: State) = ops.filter(_.eval(state))

  }
  def createItemRelationsfrom(straightSeq: Set[Operation]): Seq[ItemRelation] = {

  }
  def buildRelationMap(itemRelation: Seq[ItemRelation]): Seq[OperationRelation] {
    def findRelationBetween(o1: Operation, o2: Operation): Seq[SOP] = {
      //using itemRelation here
    }
  }

}

