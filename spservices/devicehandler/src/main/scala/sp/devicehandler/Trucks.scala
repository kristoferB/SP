package sp.devicehandler

// VOLVO trucks abilities. TODO: Move to its own node s
import akka.actor._
import sp.domain._
import sp.domain.Logic._
import java.util.UUID
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
import sp.messages._
import sp.messages.Pickles._
import scala.util.{Failure, Success, Try}
import sp.domain.logic.{PropositionParser, ActionParser}

import sp.devicehandler.{APIVirtualDevice => vdapi}
import sp.abilityhandler.{APIAbilityHandler => abapi}

object Trucks {
  def props(ahid: ID) = Props(classOf[Trucks], ahid)
}

class Trucks(ahid: ID) extends Actor with Helpers {
  import sp.abilityhandler.APIAbilityHandler.{Ability => ab}
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  // operator
  val opStartAddProduct = v("startAddProduct", "OBF_IN_Frontlid_FM 82457762_init_start")

  // load fixture 1
  val lf1ProductSensor = v("productSensor", "126,=V1UQ51+BG1_to_7")
  val lf1CloseClamps = v("closeClamps", "126,=V1UQ51+KH1-QN2S")
  val lf1OpenClamps = v("openClamps", "126,=V1UQ51+KH1-QN2R")
  val lf1ClampsClosed = v("clampsClosed", "126,=V1UQ51+UQ2.1-BGS")
  val lf1ClampsOpened = v("clampsOpened", "126,=V1UQ51+UQ2.1-BGR")

  // AR31, blue robot
  val ar31StartVacuum = v("startVacuum", "AR31_O_Vacuum_1_on")
  val ar31StopVacuum = v("stopVacuum", "AR31_O_Vacuum_1_off")
  val ar31VacuumOn = v("vacuumOn", "AR31_I_Vacuum_1_on")
  val ar31VacuumOff = v("vacuumOff", "AR31_I_Vacuum_1_off")

  val operatorVars = List(opStartAddProduct)
  val loadFixture1Vars = List(lf1ProductSensor, lf1CloseClamps, lf1OpenClamps, lf1ClampsClosed, lf1ClampsOpened)
  val ar31Vars = List(ar31StartVacuum, ar31StopVacuum, ar31VacuumOn, ar31VacuumOff)

  val allVars = operatorVars ++ loadFixture1Vars ++ ar31Vars

  val aStartAddProduct = ab("startAddProduct", UUID.randomUUID(),
    prop(allVars, "!startAddProduct && !productSensor", List("startAddProduct := true")),
    prop(allVars, "startAddProduct && !productSensor"),
    prop(allVars, "productSensor", List("startAddProduct := false")))

  val operatorAbilities = List(aStartAddProduct)

  val aCloseClamps = ab("closeClamps", UUID.randomUUID(),
    prop(allVars, "clampsOpened", List("closeClamps := true")),
    prop(allVars, "closeClamps && !clampsClosed && !clampsOpened"),
    prop(allVars, "clampsClosed", List("closeClamps := false")))

  val aOpenClamps = ab("openClamps", UUID.randomUUID(),
    prop(allVars, "clampsClosed", List("openClamps := true")),
    prop(allVars, "openClamps && !clampsClosed && !clampsOpened"),
    prop(allVars, "clampsOpened", List("openClamps := false")))

  val loadFixture1Abilities = List(aCloseClamps, aOpenClamps)

  val aStartVacuum = ab("startVacuum", UUID.randomUUID(),
    prop(allVars, "vacuumOff", List("startVacuum := true")),
    prop(allVars, "startVacuum && !vacuumOn"),
    prop(allVars, "vacuumOn", List("startVacuum := false")))

  val aStopVacuum = ab("stopVacuum", UUID.randomUUID(),
    prop(allVars, "vacuumOn", List("stopVacuum := true")),
    prop(allVars, "stopVacuum && !vacuumOff"),
    prop(allVars, "vacuumOff", List("stopVacuum := false")))

  val ar31Abilities = List(aStartVacuum, aStopVacuum)

  val allAbilities = operatorAbilities ++ loadFixture1Abilities ++ ar31Abilities
  println(allAbilities)

  // setup driver
  val driverID = UUID.randomUUID()
  def sm(vars: List[Thing]): List[vdapi.OneToOneMapper] = vars.flatMap { v =>
    v.attributes.getAs[String]("drivername").map(dn => vdapi.OneToOneMapper(v.id, driverID, dn))
  }
  val setup = SPAttributes("url" -> "opc.tcp://localhost:12686", "identifiers" -> sm(allVars).map(_.driverIdentifier))
  val driver = vdapi.Driver("opclocal", driverID, "OPCUA", setup)
  mediator ! Publish("services", SPMessage.makeJson[SPHeader, vdapi.SetUpDeviceDriver](SPHeader(from = "hej"), vdapi.SetUpDeviceDriver(driver)))

  // setup resources
  val operator = vdapi.Resource("operator", UUID.randomUUID(), operatorVars.map(_.id).toSet, sm(operatorVars), SPAttributes())
  val loadFixture1 = vdapi.Resource("loadFixture1", UUID.randomUUID(), loadFixture1Vars.map(_.id).toSet, sm(loadFixture1Vars), SPAttributes())
  val resources = List(operator, loadFixture1)

  resources.foreach { res =>
    val body = vdapi.SetUpResource(res)
    mediator ! Publish("services", SPMessage.makeJson[SPHeader, vdapi.SetUpResource](SPHeader(from = "hej"), body))
  }

  // setup abilities
  allAbilities.foreach { ab =>
    val body = abapi.SetUpAbility(ab)
    val msg = SPMessage.makeJson[SPHeader, abapi.SetUpAbility](SPHeader(to = ahid.toString, from = "hej"), body)
    mediator ! Publish("services", msg)
  }

  def receive = {
    case x => println(x)
  }

}

trait Helpers {
  def v(name: String, drivername: String) = Thing(name, SPAttributes("drivername" -> drivername))
  def prop(vars: List[IDAble], cond: String,actions: List[String] = List()) = {
    def c(condition: String): Option[Proposition] = {
      PropositionParser(vars).parseStr(condition) match {
        case Right(p) => Some(p)
        case Left(err) => println(s"Parsing failed on condition: $condition: $err"); None
      }
    }

    def a(actions: List[String]): List[Action] = {
      actions.flatMap { action =>
        ActionParser(vars).parseStr(action) match {
          case Right(a) => Some(a)
          case Left(err) => println(s"Parsing failed on action: $action: $err"); None
        }
      }
    }
    PropositionCondition(c(cond).get, a(actions))
  }
}
