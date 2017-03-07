package sp.abilityhandler

import java.util.UUID

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._
import akka.actor._
import akka.persistence._


package APIAbilityHandler {
  sealed trait Request

  case class StartAbility(id: ID, params: Map[ID, SPValue], attributes: SPAttributes) extends Request
  case class ForceResetAbility(id: ID) extends Request
  case class ForceResetAllAbilities() extends Request

  // to be used when handshake is on
  case class ExecuteCmd(cmd: ID) extends Request

  case class GetAbilities() extends Request
  case class SetUpAbility(ability: Ability, handshake: Boolean = false) extends Request


  sealed trait Response

  case class CmdID(cmd: ID) extends Response
  case class AbilityStarted(id: ID)
  case class AbilityCompleted(id: ID, result: Map[ID, SPValue])
  case class AbilityState(state: Map[ID, SPValue])


  case class Ability(name: String,
                     id: ID,
                     preCondition: PropositionCondition = PropositionCondition(AlwaysFalse, List()),
                     started: PropositionCondition = PropositionCondition(AlwaysFalse, List()),
                     postCondition: PropositionCondition = PropositionCondition(AlwaysTrue, List()),
                     resetCondition: PropositionCondition = PropositionCondition(AlwaysTrue, List()),
                     parameters: List[ID] = List(),
                     result: List[ID] = List(),
                     attributes: SPAttributes = SPAttributes())


}
import sp.abilityhandler.{APIAbilityHandler => api}


object AbilityHandler {
  def props(name: String, id: UUID, vd: UUID) = Props(classOf[AbilityHandler], name, id, vd)
}




// This actor will keep track of the abilities and parse all messages from the VD
class AbilityHandler(name: String, id: UUID, vd: UUID) extends PersistentActor with ActorLogging {
  override def persistenceId = id.toString

  override def receiveCommand = {
    case x =>
  }

  override def receiveRecover = {
    case RecoveryCompleted =>
    case x =>
  }

}

trait AbilityHandlerLogic {

}

// Internal api between abilities and handler
case class StartAbility(reqID: ID, params: Map[ID, SPValue], attributes: SPAttributes)
case object ResetAbility
case object GetIds
case class NewState(s: Map[ID, SPValue])
case object UnAvailable

case class CanNotStart(error: String)
case class AbilityStateChange(state: String, cnt: Long, reqID: Option[ID])
case class StateUpdReq(s: Map[ID, SPValue])


class AbilityActor(ability: api.Ability) extends Actor {
  override def receive = {
    case x =>
  }
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

trait AbilityActorLogic {
  val ability: api.Ability
  lazy val ids = idsFromAbility(ability)

  import AbilityState._

  var state: String = unavailable
  var count = 0
  var currentCaller = SPAttributes()





  def makeUnavailable() = state = unavailable
  def makeAvailable() = state = notEnabled

  def start(s: Map[ID, SPValue]) = {
    val theState = State(s)
    val tH = evalState(s, starting)
    if (state == starting){
      tH._2
    } else None
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
    case x if ability.started.eval(theState) && x != executing => (executing, ability.started.next(theState))
    case "starting" if ability.preCondition.eval(theState) && state != starting => (starting, ability.preCondition.next(theState))
    case "executing" if ability.postCondition.eval(theState) => (finished, ability.postCondition.next(theState))
    case "finished" if ability.resetCondition.eval(theState) => (checkEnabled(theState), ability.resetCondition.next(theState))
    case "forcedReset" => (checkEnabled(theState), ability.resetCondition.next(theState))
    case "unavailable" => (checkEnabled(theState), theState)
    case x if ability.preCondition.eval(theState) && x != starting => (enabled, theState)
    case x => (state, theState)
  }

  def checkEnabled(tS: State) = if (ability.preCondition.eval(tS)) enabled else notEnabled

  def idsFromAbility(a: api.Ability) = {
    List(a.preCondition,
      a.postCondition,
      a.started, a.resetCondition).flatMap(extractVariables) ++
      a.parameters ++ a.result
  }

  def extractVariables(p: PropositionCondition) = {
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

