package sp.devicehandler

// Labkit abilities. TODO: Move to its own node s
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



case class Labkit(ahid: ID, system: ActorSystem) extends Helpers2 {
  import sp.abilityhandler.APIAbilityHandler.{Ability => ab}
  val mediator = DistributedPubSub(system).mediator



  // Variables
  val  testingIn        = Thing("testingIn")
  val  testingInInt     = Thing("testingInInt")
  val  testingOut       = Thing("testingOut")
  val  testingOutInt    = Thing("testingOutInt")
  val  testingOutMirror = Thing("testingOutMirror")

  // resources
  val testingResource = Thing("testingResource")


  val allVars = List(testingIn, testingInInt, testingOut, testingOutInt, testingOutMirror)

  // abilities
  val a1 = abapi.Ability(name = "a1", id = ID.newID,
    preCondition = prop(allVars, s" not ${testingIn.name}", List(s"${testingIn.name} := true")),
    postCondition = prop(allVars, s"${testingOut.name}", List(s"${testingIn.name} := false")),
    started = prop(allVars, s"${testingOutMirror.name}", List())
  )

  val a2 = abapi.Ability(name = "a2", id = ID.newID,
    preCondition = prop(allVars, s"${testingInInt.name} == 2", List(s"${testingInInt.name} := 3")),
    postCondition = prop(allVars, s"${testingOutInt.name} == 5", List(s"${testingInInt.name} := 6")),
    started = prop(allVars, s"${testingOutInt.name} == 4", List()),
    resetCondition = prop(allVars, s"${testingOutInt.name} == 7", List(s"${testingInInt.name} := 2"))
  )

  val allAbilities = List(a1, a2)
  println(allAbilities)



  // setup driver
  val driverID = UUID.randomUUID()
  val opcVariables = makeTheOPCVariables(allVars, "Process_IOs.")
  val mappers = opcVariables.map(kv => vdapi.OneToOneMapper(kv._1.id, driverID, kv._2))

  val setup = SPAttributes("url" -> "opc.tcp://192.168.0.50:4840", "identifiers" -> mappers.map(_.driverIdentifier))
  val driver = vdapi.Driver("labkit", driverID, "OPCUA", setup)
  mediator ! Publish("services", SPMessage.makeJson[SPHeader, vdapi.SetUpDeviceDriver](SPHeader(from = "labkitModel"), vdapi.SetUpDeviceDriver(driver)))

  // setup resources
  val testing = vdapi.Resource(testingResource.name, testingResource.id, allVars.map(_.id).toSet, mappers, SPAttributes())

  val resources = List(testing)

  resources.foreach { res =>
    val body = vdapi.SetUpResource(res)
    mediator ! Publish("services", SPMessage.makeJson[SPHeader, vdapi.SetUpResource](SPHeader(from = "labkitModel"), body))
  }

  // setup abilities
  allAbilities.foreach { ab =>
    val body = abapi.SetUpAbility(ab)
    val msg = SPMessage.makeJson[SPHeader, abapi.SetUpAbility](SPHeader(to = ahid.toString, from = "labkitModel"), body)
    mediator ! Publish("services", msg)
  }



}

trait Helpers2 {
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

    val cRes = if (cond.isEmpty) AlwaysTrue else c(cond).get
    val aRes = a(actions)

    Condition(cRes, aRes)
  }


  val plcPath = "|var|CODESYS Control for Raspberry Pi SL.Application."
  def makeTheOPCVariables(xs: List[Thing], prefix: String = "") = {
    xs.map(t => t -> (plcPath + prefix + t.name))
  }
}
