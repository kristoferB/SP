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

  // load fixture 1
  val startAddProduct = v("startAddProduct", "OBF_IN_Frontlid_FM 82457762_init_start")
  val productSensor = v("productSensor", "126,=V1UQ51+BG1_to_7")

  val closeClamps = v("closeClamps", "126,=V1UQ51+KH1-QN2S")
  val openClamps = v("openClamps", "126,=V1UQ51+KH1-QN2R")
  val clampsClosed = v("clampsClosed", "126,=V1UQ51+UQ2.1-BGS")
  val clampsOpened = v("clampsOpened", "126,=V1UQ51+UQ2.1-BGR")

  val loadFixture1 = List(startAddProduct, productSensor, closeClamps, openClamps, clampsClosed, clampsOpened)

  val aStartAddProduct = ab("startAddProduct", UUID.randomUUID(),
    prop(loadFixture1, "!startAddProduct && !productSensor", List("startAddProduct := true")),
    prop(loadFixture1, "startAddProduct && !productSensor"),
    prop(loadFixture1, "productSensor", List("startAddProduct := true")))

  val aCloseClamps = ab("closeClamps", UUID.randomUUID(),
    prop(loadFixture1, "clampsOpened", List("closeClamps := true")),
    prop(loadFixture1, "!clampsClosed && !clampsOpened"),
    prop(loadFixture1, "clampsClosed", List("closeClamps := false")))

  val aOpenClamps = ab("openClamps", UUID.randomUUID(),
    prop(loadFixture1, "clampsClosed", List("openClamps := true")),
    prop(loadFixture1, "!clampsClosed && !clampsOpened"),
    prop(loadFixture1, "clampsOpened", List("openClamps := false")))

  val vars = loadFixture1 ++ List()
  val abilities = List(aStartAddProduct, aCloseClamps, aOpenClamps)
  println(abilities)

  val driverID = UUID.randomUUID()
  val driverStateMap: List[vdapi.OneToOneMapper] = vars.flatMap { v =>
    v.attributes.getAs[String]("drivername").map(dn => vdapi.OneToOneMapper(v.id, driverID, dn))
  }
  val resource = vdapi.Resource("R82-88", UUID.randomUUID(), driverStateMap.map(_.thing), driverStateMap, SPAttributes())
  val setup = SPAttributes("url" -> "opc.tcp://localhost:12686",
    "identifiers" -> driverStateMap.map(_.driverIdentifier))
  val driver = vdapi.Driver("opclocal", driverID, "OPCUA", setup)
  val bodyDriver = vdapi.SetUpDeviceDriver(driver)
  val bodyResource = vdapi.SetUpResource(resource)
  mediator ! Publish("services", SPMessage.makeJson[SPHeader, vdapi.SetUpDeviceDriver](SPHeader(from = "hej"), bodyDriver))
  mediator ! Publish("services", SPMessage.makeJson[SPHeader, vdapi.SetUpResource](SPHeader(from = "hej"), bodyResource))

  abilities.foreach { ab =>
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
