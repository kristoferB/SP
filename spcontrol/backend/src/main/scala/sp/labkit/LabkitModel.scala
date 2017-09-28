package sp.labkit

import akka.actor._
import sp.domain._
import sp.domain.Logic._
import java.util.UUID
import scala.util.{Failure, Success, Try}
import sp.domain.logic.{PropositionParser, ActionParser}
import sp.abilityhandler.APIAbilityHandler


object LabkitModel {
  def props(ahid: ID) = Props(classOf[Trucks], ahid)
}

class LabkitModel(ahid: ID) extends Actor with Helpers {

  // setup variables and mapping
  val cp = "|var|CODESYS Control for Raspberry Pi SL.Application.Process_IOs."
  def v(name: String) = Thing(name, SPAttributes("drivername" -> cp+name))

  val nodes = List("feeder_exec", "newCylinder_var", "pnp1_mode", "pnp1from_var", "pnp1to_var",
    "p1_mode", "p1Transport_var", "p1Process_var", "convFree_var", "convMove_var", "convAtOut_var",
    "pnp2_mode", "pnp2to3_var", "pnp2to4_var", "p3_mode", "p3Process_var", "p4_mode", "p4Process_var")

  val vars = nodes.map(v)

  // setup driver
  val driverID = UUID.randomUUID()
  def sm(vars: List[Thing]): List[APIVirtualDevice.OneToOneMapper] = vars.flatMap { v =>
    v.attributes.getAs[String]("drivername").map(dn => APIVirtualDevice.OneToOneMapper(v.id, driverID, dn))
  }
  val setup = SPAttributes("url" -> "opc.tcp://192.168.0.10:4840", "identifiers" -> sm(vars).map(_.driverIdentifier))
  val driver = APIVirtualDevice.Driver("codesys", driverID, "OPCUA", setup)
  mediator ! Publish(APIVirtualDevice.topicRequest, SPMessage.makeJson[SPHeader, APIVirtualDevice.SetUpDeviceDriver](SPHeader(from = "hej"), APIVirtualDevice.SetUpDeviceDriver(driver)))

  // setup resource
  val labkit = APIVirtualDevice.Resource("labkit", UUID.randomUUID(), vars.map(_.id).toSet, sm(vars), SPAttributes())
  val resources = List(labkit)

  resources.foreach { res =>
    val body = APIVirtualDevice.SetUpResource(res)
    mediator ! Publish(APIVirtualDevice.topicRequest, SPMessage.makeJson[SPHeader, APIVirtualDevice.SetUpResource](SPHeader(from = "hej"), body))
  }

  def receive = {
    case x => println(x)
  }

}
