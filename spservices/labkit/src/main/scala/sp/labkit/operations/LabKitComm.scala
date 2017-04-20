package sp.labkit.operations


import sp.domain._
import sp.domain.Logic._
import sp.messages._
import Pickles._
import scala.util._




package APILabkitControl {
  sealed trait Request
  sealed trait Response

  case class SomeNice() extends Request
  case class SomeResp() extends Response

  // Här lägger ni in ett api för frontend att styra systemet



  object attributes {
    val service = "LabKitControl"
  }


}

import sp.labkit.operations.{APILabkitControl => api}
import sp.labkit.operations.{APIAbilityHandler => abAPI}
import sp.labkit.operations.{APIVirtualDevice => vdAPI}
import sp.labkit.operations.{API_OperationRunner => opAPI}

object LabKitComm {
  def extractRequest(mess: Try[SPMessage]) = for {
      m <- mess
      h <- m.getHeaderAs[SPHeader] if h.to == api.attributes.service
      b <- m.getBodyAs[api.Request]
    } yield (h, b)

  def extractOPReply(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] if h.to == api.attributes.service
    b <- m.getBodyAs[opAPI.Response]
    } yield (h, b)

  def extractServiceRequest(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP.StatusRequest]
    } yield (h, b)


  def makeMess(h: SPHeader, b: api.Response) = SPMessage.makeJson[SPHeader, api.Response](h, b)
  def makeMess(h: SPHeader, b: vdAPI.Request) = SPMessage.makeJson[SPHeader, vdAPI.Request](h, b)
  def makeMess(h: SPHeader, b: opAPI.Request) = SPMessage.makeJson[SPHeader, opAPI.Request](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)

}


package API_OperationRunner {
  sealed trait Request
  sealed trait Response

  case class Setup(name: String, runnerID: ID, ops: Set[Operation], opAbilityMap: Map[ID, ID], initialState: Map[ID, SPValue]) extends Request
  case class SetState(runnerID: ID, state: Map[ID, SPValue]) extends Request
  case class ForceComplete(ability: ID) extends Request
  case class TerminateRunner(runnerID: ID) extends Request
  case class GetState(runnerID: ID) extends Request
  case class GetRunners() extends Request

  case class StateEvent(runnerID: ID, state: Map[ID, SPValue]) extends Response
  case class Runners(ids: List[Setup]) extends Response


  object attributes {
    val service = "OperationRunner"
    val version = 1.0
  }
}


package APIAbilityHandler {
  sealed trait Request
  sealed trait Response

  case class StartAbility(id: ID, params: Map[ID, SPValue] = Map(), attributes: SPAttributes = SPAttributes()) extends Request
  case class ForceResetAbility(id: ID) extends Request
  case class ForceResetAllAbilities() extends Request

  // to be used when handshake is on
  case class ExecuteCmd(cmd: ID) extends Request

  case class GetAbilities() extends Request
  case class SetUpAbility(ability: Ability, handshake: Boolean = false) extends Request



  case class CmdID(cmd: ID) extends Response
  case class AbilityStarted(id: ID) extends Response
  case class AbilityCompleted(id: ID, result: Map[ID, SPValue]) extends Response
  case class AbilityState(id: ID, state: Map[ID, SPValue]) extends Response
  case class Abilities(xs: List[Ability]) extends Response
  case class Abs(a: List[(ID,String)]) extends Response

  case class Ability(name: String,
                     id: ID,
                     preCondition: PropositionCondition = PropositionCondition(AlwaysFalse, List()),
                     started: PropositionCondition = PropositionCondition(AlwaysFalse, List()),
                     postCondition: PropositionCondition = PropositionCondition(AlwaysTrue, List()),
                     resetCondition: PropositionCondition = PropositionCondition(AlwaysTrue, List()),
                     parameters: List[ID] = List(),
                     result: List[ID] = List(),
                     attributes: SPAttributes = SPAttributes()) extends Response

  object attributes {
    val service = "abilityHandler"
  }
}

package APIVirtualDevice {
  sealed trait Request
  sealed trait Response
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
  case class StateEvent(resource: String, id: ID, state: Map[ID, SPValue], diff: Boolean = false) extends Response

  case class Resources(xs: List[Resource]) extends Response
  case class Drivers(xs: List[Driver]) extends Response
  case class NewResource(x: Resource) extends Response
  case class RemovedResource(x: Resource) extends Response
  case class NewDriver(x: Driver) extends Response
  case class RemovedDriver(x: Driver) extends Response

  case class Resource(name: String, id: ID, things: Set[ID], stateMap: List[DriverStateMapper], setup: SPAttributes, sendOnlyDiffs: Boolean = false)
  case class Driver(name: String, id: ID, driverType: String, setup: SPAttributes)


  object  attributes {
    val service = "virtualDevice"
  }
}