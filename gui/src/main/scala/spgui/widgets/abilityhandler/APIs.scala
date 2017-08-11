package spgui.widgets.abilityhandler

import java.util.UUID
import sp.domain._
import sp.domain.Logic._

object APIVirtualDevice {
  sealed trait Request
  val service = "VirtualDevice"

  // requests setup
  case class SetUpDeviceDriver(driver: Driver) extends Request
  case class SetUpResource(resource: Resource) extends Request
  case class GetResources() extends Request

  sealed trait DriverStateMapper
  case class OneToOneMapper(thing: ID, driverID: ID, driverIdentifier: String) extends DriverStateMapper

  // requests command (gets a SPACK and when applied, SPDone (and indirectly a StateEvent))
  case class ResourceCommand(resource: ID, stateRequest: Map[ID, SPValue], timeout: Int = 0) extends Request

  // requests from driver
  case class DriverStateChange(name: String, id: ID, state: Map[String, SPValue], diff: Boolean = false) extends Request
  case class DriverCommand(name: String, id: ID, state: Map[String, SPValue]) extends Request
  case class DriverCommandDone(requestID: ID, result: Boolean) extends Request

  // answers
  sealed trait Response
  case class StateEvent(resource: String, id: ID, state: Map[ID, SPValue], diff: Boolean = false) extends Response

  case class Resources(xs: List[Resource]) extends Response
  case class Drivers(xs: List[Driver]) extends Response
  case class NewResource(x: Resource) extends Response
  case class RemovedResource(x: Resource) extends Response
  case class NewDriver(x: Driver) extends Response
  case class RemovedDriver(x: Driver) extends Response

  case class Resource(name: String, id: ID, things: Set[ID], stateMap: List[DriverStateMapper], setup: SPAttributes, sendOnlyDiffs: Boolean = false)
  case class Driver(name: String, id: ID, driverType: String, setup: SPAttributes)


  implicit lazy val fDriver: JSFormat[Driver] = deriveFormatISA[Driver]
  implicit lazy val fResource: JSFormat[Resource] = deriveFormatISA[Resource]
  object Request {
    implicit lazy val fVirtualDeviceRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fVirtualDeviceResponse: JSFormat[Response] = deriveFormatISA[Response]
  }
  object DriverStateMapper {
    implicit lazy val fDriverStateMapper: JSFormat[DriverStateMapper] = deriveFormatISA[DriverStateMapper]
  }

}

object APIAbilityHandler {
  sealed trait Request
  sealed trait Response
  val service = "abilityHandler"

  case class StartAbility(id: ID, params: Map[ID, SPValue] = Map(), attributes: SPAttributes = SPAttributes()) extends Request
  case class ForceResetAbility(id: ID) extends Request
  case object ForceResetAllAbilities extends Request

  // to be used when handshake is on
  case class ExecuteCmd(cmd: ID) extends Request

  case object GetAbilities extends Request
  case class SetUpAbility(ability: Ability, handshake: Boolean = false) extends Request



  case class CmdID(cmd: ID) extends Response
  case class AbilityStarted(id: ID) extends Response
  case class AbilityCompleted(id: ID, result: Map[ID, SPValue]) extends Response
  case class AbilityState(id: ID, state: Map[ID, SPValue]) extends Response
  case class Abilities(xs: List[Ability]) extends Response
  case class Abs(a: List[(ID,String)]) extends Response

  case class Ability(name: String,
                     id: ID,
                     preCondition: Condition = Condition(AlwaysFalse, List()),
                     started: Condition = Condition(AlwaysFalse, List()),
                     postCondition: Condition = Condition(AlwaysTrue, List()),
                     resetCondition: Condition = Condition(AlwaysTrue, List()),
                     parameters: List[ID] = List(),
                     result: List[ID] = List(),
                     attributes: SPAttributes = SPAttributes())


  implicit lazy val fAbility: JSFormat[Ability] = deriveFormatISA[Ability]
  //implicit lazy val fAbilityState: JSReads[AbilityState] = deriveReadISA[AbilityState]
  object Request {
    implicit lazy val fExampleServiceRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fExampleServiceResponse: JSFormat[Response] = deriveFormatISA[Response]
  }
}