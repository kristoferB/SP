package sp.abilityhandler

import java.util.UUID

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._
import akka.actor._
import akka.persistence._

import scala.util.Try



import sp.abilityhandler.{APIAbilityHandler => api}
import sp.devicehandler.{APIVirtualDevice => vdAPI}


object AbilityHandler {
  def props(name: String, id: UUID, vd: UUID) = Props(classOf[AbilityHandler], name, id, vd)
}




// This actor will keep track of the abilities and parse all messages from the VD
class AbilityHandler(name: String, handlerID: UUID, vd: UUID) extends PersistentActor with ActorLogging with AbilityLogic {
  override def persistenceId = handlerID.toString
  case class AbilityStorage(ability: api.Ability, actor: ActorRef, ids: Set[ID] = Set(), current: Option[AbilityStateChange] = None)

  var abilities: Map[ID, AbilityStorage] = Map()
  var resources: List[vdAPI.Resource] = List()
  var state: Map[ID, SPValue] = Map()

  import context.dispatcher
  import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put, Subscribe}
  val mediator = akka.cluster.pubsub.DistributedPubSub(context.system).mediator

  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("events", self)
  mediator ! Subscribe("answers", self)


  val getResources = AbilityComm.makeMess(SPHeader(from = handlerID.toString, to = vd.toString), vdAPI.GetResources())
  mediator ! Publish("services", getResources)

  override def receiveCommand = {

    case x: String => handleRequests(x)

      // move this to a partial function
    case CanNotStart(req, abID, error) =>
      val h = SPHeader(from = handlerID.toString)
      mediator ! Publish("answers", AbilityComm.makeMess(h, APISP.SPError(s"ability $abID couldn't start. $error")))
      mediator ! Publish("answers", AbilityComm.makeMess(h, APISP.SPDone()))

    case x @ AbilityStateChange(abID, s, cnt, req) =>
      val h = SPHeader(from = handlerID.toString, reqID = req.getOrElse(ID.newID))
      abilities.get(abID).foreach{ as =>
        abilities += abID -> as.copy(current = Some(x))
      }
      val abilityState = SPAttributes(
        "state" -> s,
        "counter" -> cnt
      )
      val b = api.AbilityState(abID, Map(abID -> abilityState))
      mediator ! Publish("events", AbilityComm.makeMess(h, b))

      req.foreach{ req =>
        val res = s match {
          case "executing" =>
            mediator ! Publish("answers", AbilityComm.makeMess(h, api.AbilityStarted(abID)))
          case "finished" =>
            mediator ! Publish("answers", AbilityComm.makeMess(h, api.AbilityCompleted(abID, Map())))
            mediator ! Publish("answers", AbilityComm.makeMess(h, APISP.SPDone()))
          case _ => Unit

        }

      }

    case StateUpdReq(abID, s) =>
      val res = resources.filter(r => r.things.intersect(s.keySet).nonEmpty)
      val toSend = res.map{r =>
        val h = SPHeader(from = handlerID.toString, to = vd.toString, reply = SPValue(handlerID))
        val b = vdAPI.ResourceCommand(r.id, s.filter(kv => r.things.contains(kv._1)))
        mediator ! Publish("services", AbilityComm.makeMess(h, b))
      }

    case StateIsMissingIDs(abID, ids) =>
      val h = SPHeader(from = handlerID.toString)

      mediator ! Publish("spevents", AbilityComm.makeMess(h, APISP.SPError("Ability has ids that is not found in the state. Either the VD is unavailible or something is wrong",
        SPAttributes("ability" -> abilities.get(abID).map(_.ability.name).getOrElse("missing name"),
        "id" -> abID, "missingThings" -> ids)
      )))

  }

  override def receiveRecover = {
    case RecoveryCompleted =>
    case x =>
  }


  def handleRequests(x: String): Unit = {
    val mess = SPMessage.fromJson(x)

    matchRequests(mess)
    matchVDMessages(mess)
    matchServiceRequests(mess)


  }


  def matchRequests(mess: Try[SPMessage]) = {
    AbilityComm.extractRequest(mess, handlerID, name) map { case (h, b) =>
      val updH = h.copy(from = h.to, to = "")

      // Message was to me so i send an SPACK
      mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPACK()))

      b match {
        case api.StartAbility(id, params, attr) =>
          abilities.get(id) match {
            case Some(a) =>
              a.actor ! StartAbility(state, h.reqID, params, attr)
            case None =>
              mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPError(s"ability $id does not exists in this handler")))
              mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPDone()))

          }

        case api.ForceResetAbility(id) =>
          abilities.get(id) match {
            case Some(a) =>
              a.actor ! ResetAbility(state)
            case None =>
              mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPError(s"ability $id does not exists in this handler")))
          }
          mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPDone()))

        case x: api.ForceResetAllAbilities =>
          val r = ResetAbility(state)
          abilities.foreach(kv => kv._2.actor ! r)
          mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPDone()))

        case api.ExecuteCmd(cmd) =>
        // to be implemented

        case x: api.GetAbilities =>
          val xs = abilities.map(_._2.ability).toList

          val abs = abilities.map(a=>(a._2.ability.id,a._2.ability.name)).toList

          println("got getabilitiies request")
          mediator ! Publish("answers", AbilityComm.makeMess(updH, api.Abilities(xs)))
          mediator ! Publish("answers", AbilityComm.makeMess(updH, api.Abs(abs)))

          mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPDone()))

          abilities.foreach(a => a._2.actor ! GetState)


        case api.SetUpAbility(ab, hand) =>
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
          mediator ! Publish("answers", AbilityComm.makeMess(updH, APISP.SPDone()))
      }
    }
  }

  def filterState(ids: Set[ID], state: Map[ID, SPValue]) = state.filter(kv => ids.contains(kv._1))

  def matchVDMessages(mess: Try[SPMessage]) = {
    AbilityComm.extractVDReply(mess, handlerID, vd.toString) map { case (h, b) =>
      b match {
        case vdAPI.StateEvent(r, rID, s, d) =>
          state = state ++ s
          val f = abilities.filter(kv => kv._2.ids.intersect(s.keySet).nonEmpty)
          f.foreach{kv => kv._2.actor ! NewState(filterState(kv._2.ids, state))}
        case vdAPI.Resources(xs) =>
          resources = xs
        case x =>
      }
    }

  }

  val info = SPAttributes(
    "service" -> api.attributes.service,
    "name" -> name,
    "instanceID" -> handlerID,
    "group" -> "runtime",
    "attributes" -> SPAttributes("vd" -> vd)
  )

  def matchServiceRequests(mess: Try[SPMessage]) = {
    AbilityComm.extractServiceRequest(mess) map { case (h, b) =>
      val spHeader = h.copy(from = handlerID.toString)
      mediator ! Publish("spevents", AbilityComm.makeMess(spHeader, APISP.StatusResponse(info)))
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
  def props(ability: api.Ability) =
    Props(classOf[AbilityActor], ability)
}

class AbilityActor(val ability: api.Ability) extends Actor with AbilityActorLogic{
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
  val ability: api.Ability
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
    val theState = State(s)
    val aS = if (force.isEmpty) state else force
    val abilityState = updateState(aS, theState)

    val newAState = if (state != abilityState._1) Some(abilityState._1) else None
    val newRState = if (theState != abilityState._2) Some(abilityState._2.state) else None

    newAState.foreach(x => state = x)
    (newAState, newRState)
  }

  def updateState(s: String, theState: State): (String, State) = s match {
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

  def checkEnabled(tS: State) = if (ability.preCondition.eval(tS)) enabled else notEnabled



  def createNotStartingErrorMessage() = {
    s"state: $state"
  }

}

trait AbilityLogic {


  def idsFromAbility(a: api.Ability) = {
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
