package sp.abilityhandler

import java.util.UUID

import sp.domain._
import sp.domain.Logic._
import akka.actor._
import akka.persistence._

import scala.util.Try



object AbilityHandler {
  def props(name: String, id: UUID, vd: UUID) = Props(classOf[AbilityHandler], name, id, vd)

  import sp.domain.SchemaLogic._

  case class AbilityHandlerRequest(request: APIAbilityHandler.Request)
  case class AbilityHandlerResponse(response: APIAbilityHandler.Response)

  val sres: com.sksamuel.avro4s.SchemaFor[AbilityHandlerResponse] = com.sksamuel.avro4s.SchemaFor[AbilityHandlerResponse]
  val sreq: com.sksamuel.avro4s.SchemaFor[AbilityHandlerRequest] = com.sksamuel.avro4s.SchemaFor[AbilityHandlerRequest]

  val apischema = makeMeASchema(
    sreq(),
    sres()
  )


  val attributes: APISP.StatusResponse = APISP.StatusResponse(
    service = APIAbilityHandler.service,
    instanceID = Some(ID.newID),
    instanceName = "",
    tags = List("ability", "virtual device", "vd", "runtime", "communication"),
    api = apischema,
    version = 1,
    attributes = SPAttributes.empty
  )
}


import sp.abilityhandler.{APIAbilityHandler => abapi}
import sp.devicehandler.{APIVirtualDevice => vdapi}

trait AbilityComm {

  def extractRequest(mess: Option[SPMessage], instanceID: ID, name: String) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader] if h.to == instanceID.toString || h.to == name || h.to == AbilityHandler.attributes.service
      b <- m.getBodyAs[abapi.Request]
    } yield (h, b)

  def extractVDReply(mess: Option[SPMessage], instanceID: ID, vd: String) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] if h.from.contains(vd) || h.reply == SPValue(instanceID)
    b <- m.getBodyAs[vdapi.Response]
    } yield (h, b)

  def extractServiceRequest(mess: Option[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP] if b == APISP.StatusRequest
    } yield {
    (h, b)
  }


  def makeMess(h: SPHeader, b: abapi.Response) = SPMessage.makeJson[SPHeader, abapi.Response](h, b)
  def makeMess(h: SPHeader, b: vdapi.Request) = SPMessage.makeJson[SPHeader, vdapi.Request](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)
}
// This actor will keep track of the abilities and parse all messages from the VD
class AbilityHandler(name: String, handlerID: UUID, vd: UUID) extends PersistentActor
    with ActorLogging
    with AbilityLogic
    with AbilityComm
    with sp.service.ServiceCommunicationSupport
    with sp.service.MessageBussSupport {

  override def persistenceId = handlerID.toString
  case class AbilityStorage(ability: APIAbilityHandler.Ability, actor: ActorRef, ids: Set[ID] = Set(), current: Option[AbilityStateChange] = None)

  var abilities: Map[ID, AbilityStorage] = Map()
  var resources: List[vdapi.Resource] = List()
  var state: Map[ID, SPValue] = Map()

  import context.dispatcher

  subscribe(abapi.topicRequest)

  // Setting up the status response that is used for identifying the service in the cluster
  val statusResponse = AbilityHandler.attributes.copy(
    instanceName = this.name,
    instanceID = Some(handlerID),
    attributes = SPAttributes("vd" -> vd)
  )

  // starts wiating for ping requests from service handler
  triggerServiceRequestComm(statusResponse)

  subscribe(vdapi.topicResponse)
  subscribe(abapi.topicRequest)

  val getResources = makeMess(SPHeader(from = handlerID.toString, to = vd.toString), vdapi.GetResources)
  publish("services", getResources)

  override def receiveCommand = {

    case x: String => handleRequests(x)

      // move this to a partial function
    case CanNotStart(req, abID, error) =>
      val h = SPHeader(from = handlerID.toString)
      publish(abapi.topicResponse, makeMess(h, APISP.SPError(s"ability $abID couldn't start. $error")))
      publish(abapi.topicResponse, makeMess(h, APISP.SPDone()))

    case x @ AbilityStateChange(abID, s, cnt, req) =>
      val h = SPHeader(from = handlerID.toString, reqID = req.getOrElse(ID.newID))
      abilities.get(abID).foreach{ as =>
        abilities += abID -> as.copy(current = Some(x))
      }
      val abilityState = SPAttributes(
        "state" -> s,
        "counter" -> cnt
      )
      val b = abapi.AbilityState(abID, Map(abID -> abilityState))
      // hur ska vi ha det med event/answer-topics?
      // publish("events", makeMess(h, b))
      publish(abapi.topicResponse, makeMess(h, b))

      req.foreach{ req =>
        val res = s match {
          case "executing" =>
            publish(abapi.topicResponse, makeMess(h, abapi.AbilityStarted(abID)))
          case "finished" =>
            publish(abapi.topicResponse, makeMess(h, abapi.AbilityCompleted(abID, Map())))
            publish(abapi.topicResponse, makeMess(h, APISP.SPDone()))
          case _ => Unit

        }

      }

    case StateUpdReq(abID, s) =>
      val res = resources.filter(r => r.things.intersect(s.keySet).nonEmpty)
      val toSend = res.map{r =>
        val h = SPHeader(from = handlerID.toString, to = vd.toString, reply = SPValue(handlerID))
        val b = vdapi.ResourceCommand(r.id, s.filter(kv => r.things.contains(kv._1)))
        publish("services", makeMess(h, b))
      }

    case StateIsMissingIDs(abID, ids) =>
      val h = SPHeader(from = handlerID.toString)
      val errorAttr = SPAttributes(
        "ability" -> abilities.get(abID).map(_.ability.name),
        "id" -> abID, "missingThings" -> ids)

      publish("spevents", makeMess(h,
        APISP.SPError("Ability has ids that is not found in the state. Either the VD is unavailable or something is wrong",
        errorAttr)
      ))

  }

  override def receiveRecover = {
    case RecoveryCompleted =>
    case x =>
  }


  def handleRequests(x: String): Unit = {
    val mess = SPMessage.fromJson(x)

    matchRequests(mess)
    matchVDMessages(mess)

  }


  def matchRequests(mess: Option[SPMessage]) = {
    extractRequest(mess, handlerID, name) map { case (h, b) =>
      val updH = h.copy(from = h.to, to = "")

      // Message was to me so i send an SPACK
      publish(abapi.topicResponse, makeMess(updH, APISP.SPACK()))

      b match {
        case abapi.StartAbility(id, params, attr) =>
          abilities.get(id) match {
            case Some(a) =>
              a.actor ! StartAbility(state, h.reqID, params, attr)
            case None =>
              publish(abapi.topicResponse, makeMess(updH, APISP.SPError(s"ability $id does not exists in this handler")))
              publish(abapi.topicResponse, makeMess(updH, APISP.SPDone()))

          }

        case abapi.ForceResetAbility(id) =>
          abilities.get(id) match {
            case Some(a) =>
              a.actor ! ResetAbility(state)
            case None =>
              publish(abapi.topicResponse, makeMess(updH, APISP.SPError(s"ability $id does not exists in this handler")))
          }
          publish(abapi.topicResponse, makeMess(updH, APISP.SPDone()))

        case abapi.ForceResetAllAbilities =>
          val r = ResetAbility(state)
          abilities.foreach(kv => kv._2.actor ! r)
          publish(abapi.topicResponse, makeMess(updH, APISP.SPDone()))

        case abapi.ExecuteCmd(cmd) =>
        // to be implemented

        case abapi.GetAbilities =>
          val xs = abilities.map(_._2.ability).toList

          val abs = abilities.map(a=>(a._2.ability.id,a._2.ability.name)).toList

          println("got getabilitiies request")
          publish(abapi.topicResponse, makeMess(updH, abapi.Abilities(xs)))
          publish(abapi.topicResponse, makeMess(updH, abapi.Abs(abs)))

          publish(abapi.topicResponse, makeMess(updH, APISP.SPDone()))

          abilities.foreach(a => a._2.actor ! GetState)


        case abapi.SetUpAbility(ab, hand) =>
          println("--------------------------------------------------")
          println("--------------------------------------------------")
          println("--------------------------------------------------")
          println("--------------------------------------------------")
          println("--------------------------------------------------")
          println("--------------------------------------------------")
          println("--------------------------------------------------")
          println("--------------------------------------------------")
          println("--------------------------------------------------")
          println(ab)
          val ids = idsFromAbility(ab)
          val act = context.actorOf(AbilityActor.props(ab))
          abilities += ab.id -> AbilityStorage(ab, act, ids)
          act ! NewState(filterState(ids, state))
          publish(abapi.topicResponse, makeMess(updH, APISP.SPDone()))
      }
    }
  }

  def filterState(ids: Set[ID], state: Map[ID, SPValue]) = state.filter(kv => ids.contains(kv._1))

  def matchVDMessages(mess: Option[SPMessage]) = {
    extractVDReply(mess, handlerID, vd.toString) map { case (h, b) =>
      b match {
        case vdapi.StateEvent(r, rID, s, d) =>
          state = state ++ s
          val f = abilities.filter(kv => kv._2.ids.intersect(s.keySet).nonEmpty)
          f.foreach{kv => kv._2.actor ! NewState(filterState(kv._2.ids, state))}
        case vdapi.Resources(xs) =>
          resources = xs
        case x =>
      }
    }

  }

}




// Internal api between abilities and handler
case class StartAbility(state: Map[ID, SPValue], reqID: ID, params: Map[ID, SPValue], attributes: SPAttributes = SPAttributes())
case class ResetAbility(state: Map[ID, SPValue])
case object GetIds
case class NewState(s: Map[ID, SPValue])
case object UnAvailable
case object GetState

case class AbilityIds(ability: ID, ids: Set[ID])
case class CanNotStart(reqID: ID, ability: ID, error: String)
case class AbilityStateChange(ability: ID, state: String, cnt: Long, reqID: Option[ID])
case class StateIsMissingIDs(ability: ID, xs: Set[ID])
case class StateUpdReq(ability: ID, state: Map[ID, SPValue])


object AbilityActor {
  def props(ability: APIAbilityHandler.Ability) =
    Props(classOf[AbilityActor], ability)
}

class AbilityActor(val ability: APIAbilityHandler.Ability) extends Actor with AbilityActorLogic{
  var reqID: Option[ID] = None

  override def receive = {
    case GetIds => sender() ! AbilityIds(ability.id, ids)
    case UnAvailable =>
      makeUnavailable()
      sendAbilityState(sender())

    case StartAbility(s, id, p, attr) =>
      println("STARTING ABILITY")
      val res = start(s)
      res.foreach { updS =>
        reqID = Some(id)
        if (p.nonEmpty) sender() ! StateUpdReq(ability.id, p)
        sender() ! StateUpdReq(ability.id, updS)
        sendAbilityState(sender())
        "fix timeout here if needed"
      }
      if (res.isEmpty)
        sender() ! CanNotStart(id, ability.id, createNotStartingErrorMessage())

    case ResetAbility(s) =>
      val res = reset(s)
      res.foreach { updS =>
        sender() ! StateUpdReq(ability.id, updS)
      }
      sendAbilityState(sender())

    case NewState(s) =>
      val missingIDs = ids.diff(s.keySet)
      if (missingIDs.nonEmpty){
        sender() ! StateIsMissingIDs(ability.id, missingIDs)
      }

      val res = evalState(s)
      res._1.foreach { updS =>
        sendAbilityState(sender())
      }
      res._2.foreach{ updS =>
        sender() ! StateUpdReq(ability.id, updS)
      }
    case GetState =>
      sendAbilityState(sender())
  }

  def sendAbilityState(to: ActorRef) =
    to ! AbilityStateChange(ability.id, state, count, reqID)



}









// The various states that an ability can be in
object AbilityState {
    val unavailable = "unavailable"
    val notEnabled = "notEnabled"
    val enabled = "enabled"
    val starting = "starting"
    val executing = "executing"
    val finished = "finished"
    val forcedReset = "forcedReset"
}

trait AbilityActorLogic extends AbilityLogic{
  val ability: APIAbilityHandler.Ability
  lazy val ids = idsFromAbility(ability)

  import AbilityState._

  var state: String = unavailable
  var count = 0
  var currentCaller = SPAttributes()


  def makeUnavailable() = state = unavailable
  def makeAvailable() = state = notEnabled

  def start(s: Map[ID, SPValue]) = {
    val tH = evalState(s, starting)
    if (state == starting){
      tH._2
    } else None
  }

  def reset(s: Map[ID, SPValue]) = {
    val tH = evalState(s, forcedReset)
    tH._2
  }


  def evalState(s: Map[ID, SPValue], force: String = "") = {
    val theState = SPState(state = s)
    val aS = if (force.isEmpty) state else force
    val abilityState = updateState(aS, theState)

    val newAState = if (state != abilityState._1) Some(abilityState._1) else None
    val newRState = if (theState != abilityState._2) Some(abilityState._2.state) else None

    newAState.foreach(x => state = x)
    (newAState, newRState)
  }

  def updateState(s: String, theState: SPState): (String, SPState) = s match {
    case "starting" if ability.preCondition.eval(theState) && state != starting => (starting, ability.preCondition.next(theState))
    case "forcedReset" if state != forcedReset=> (forcedReset, ability.resetCondition.next(theState))
    case "forcedReset" => (checkEnabled(theState), ability.resetCondition.next(theState))
    case "executing" if ability.postCondition.eval(theState) => (finished, ability.postCondition.next(theState))
    case x if ability.started.eval(theState) && x != executing =>
      count += 1
      (executing, ability.started.next(theState))
    case "finished" if ability.resetCondition.eval(theState) => (checkEnabled(theState), ability.resetCondition.next(theState))
    case "unavailable" => (checkEnabled(theState), theState)
    case x if ability.preCondition.eval(theState) && state != starting => (enabled, theState)
    case x => (state, theState)
  }

  def checkEnabled(tS: SPState) = if (ability.preCondition.eval(tS)) enabled else notEnabled



  def createNotStartingErrorMessage() = {
    s"state: $state"
  }

}

trait AbilityLogic {


  def idsFromAbility(a: APIAbilityHandler.Ability) = {
    Set(a.preCondition,
      a.postCondition,
      a.started, a.resetCondition).flatMap(extractVariables) ++
      a.parameters ++ a.result
  }

  def extractVariables(p: Condition) = {
    fromGuard(p.guard) ++ fromAction(p.action)
  }

  def fromGuard(p: Proposition): List[ID] = {
    p match {
      case AND(xs) => xs.flatMap(fromGuard)
      case OR(xs) => xs.flatMap(fromGuard)
      case NOT(x) => fromGuard(x)
      case pe: PropositionEvaluator =>
        val xs = List(pe.left, pe.right)
        xs.collect{
          case SVIDEval(id) => id

        }
      case x => List()
    }
  }


  def fromAction(a: List[Action]): List[ID] = {
    a.map(_.id) ++  a.map(_.value).collect{
      case ASSIGN(id) => id
    }
  }


}
