package sp.abilityhandler

import java.util.UUID

import sp.domain._
import sp.messages._
import Pickles._
import akka.actor._
import akka.persistence._


package APIAbilityHandler {
  sealed trait Request

  case class StartAbility(id: ID, params: Map[ID, SPValue], attributes: SPAttributes) extends Request
  case class ForceResetAbility(id: ID) extends Request
  case class ForceResetAllAbilities() extends Request

  // to be used when handskake is on
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
                     resetAction: Option[Action] = None,
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

// Create one actor per ability to keep track of the state and
// simplify for example timeouts
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
  val executing = "started"
  val finished = "completed"
  val forcedReset = "forcedReset"
  val resetCompleted = "resetCompleted"

  case class State(state: String, count: Long, currentCalled: SPAttributes)
}

trait AbilityActorLogic {
  val ability: api.Ability
  var state: AbilityState.State

  def getAllVariables = {

  }

  def makeUnavailable() = state = state.copy(state = AbilityState.unavailable)



  def extractVariables(p: PropositionCondition) = {
    var updGVars = Map[ID, Set[SPValue]]()
    var updAVars = Map[ID, Set[SPValue]]()

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


    def fromAction(a: Action): List[ID] = {
      a.id ::  List(a.value).collect{
        case ASSIGN(id) => id
      }
    }



  }

}

