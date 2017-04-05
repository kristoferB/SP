package sp.devicehandler

// Labkit abilities. TODO: Move to its own node s
import java.util.UUID

import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import sp.abilityhandler.{APIAbilityHandler => abapi}
import sp.devicehandler.{APIVirtualDevice => vdapi}
import sp.domain.Logic._
import sp.domain._
import sp.domain.logic.{ActionParser, PropositionParser}
import sp.messages.Pickles._



case class Labkit(ahid: ID, system: ActorSystem) extends Helpers2 {
  import sp.abilityhandler.APIAbilityHandler.{Ability => ab}
  val mediator = DistributedPubSub(system).mediator



  // Variables
  val  testingIn        = Thing("testingIn")
  val  testingInInt     = Thing("testingInInt")
  val  testingOut       = Thing("testingOut")
  val  testingOutInt    = Thing("testingOutInt")
  val  testingOutMirror = Thing("testingOutMirror")
  val  feedRun          = Thing("feedRun")
  val  feedSensor       = Thing("feedSensor")
  val  feedState        = Thing("feedState")
  val  c1p1Run          = Thing("c1p1Run")
  val  c1p1Dir          = Thing("c1p1Dir")
  val  c1p1State        = Thing("c1p1State")
  val  c1p1Sensor       = Thing("c1p1Sensor")
  val  c1p2Run          = Thing("c1p2Run")
  val  c1p2Dir          = Thing("c1p2Dir")
  val  c1p2State        = Thing("c1p2State")
  val  c1p2Sensor       = Thing("c1p2Sensor")
  val  robot1Run        = Thing("robot1Run")
  val  robot1Target     = Thing("robot1Target")
  val  robot1gripping   = Thing("robot1gripping")
  val  robot1State      = Thing("robot1State")
  val  robot1ResetRun   = Thing("robot1ResetRun")
  val  robot1ResetState = Thing("robot1ResetState")

  // resources
  val testingResource = Thing("testingResource")


  val allVars = List(testingIn, testingInInt, testingOut, testingOutInt, testingOutMirror, feedRun, feedSensor, feedState, c1p1Run, c1p1Dir,
    c1p1State, c1p1Sensor, c1p2Run, c1p2Dir, c1p2State, c1p2Sensor, robot1Run, robot1Target, robot1State, robot1ResetRun, robot1ResetState,
    robot1gripping)

  // abilities
  val a1 = abapi.Ability(name = "a1", id = ID.newID,
    preCondition = prop(allVars, s"1 == 1", List(s"${robot1Target.name} := 5")),
    postCondition = prop(allVars, s"${testingOut.name}", List(s"${testingIn.name} := false")),
    started = prop(allVars, s"${testingOutMirror.name}", List())
  )

  val a2 = abapi.Ability(name = "a2", id = ID.newID,
    preCondition = prop(allVars, s"${testingInInt.name} == 2", List(s"${testingInInt.name} := 3")),
    postCondition = prop(allVars, s"${testingOutInt.name} == 5", List(s"${testingInInt.name} := 6")),
    started = prop(allVars, s"${testingOutInt.name} == 4", List()),
    resetCondition = prop(allVars, s"${testingOutInt.name} == 7", List(s"${testingInInt.name} := 2"))
  )

  val feeder = abapi.Ability(name = "feeder", id = ID.newID,
    preCondition = prop(allVars, s" not ${feedRun.name} and not ${feedSensor.name} and ${feedState.name} == 1", List(s"${feedRun.name} := true")),
    postCondition = prop(allVars, s"${feedState.name} == 3", List(s"${feedRun.name} := false")),
    started = prop(allVars, s"${feedState.name} == 2", List())
  )

  val conv1proc1 = abapi.Ability(name = "conv1proc1", id = ID.newID,
    preCondition = prop(allVars, s"${c1p1State.name} == 1 and not ${c1p1Run.name} and not ${c1p1Sensor.name} and ${c1p2State.name} == 1",
      List(s"${c1p1Run.name} := true", s"${c1p1Dir.name} := true")), //Not sure if we are supposed to set direction here or somewhere else, now it moves right
    postCondition = prop(allVars, s"${c1p1State.name} == 3", List(s"${c1p1Run.name} := false", s"${c1p1Dir.name} := false)")),
    started = prop(allVars, s"${c1p1State.name} == 2", List())
  )
  val conv1proc2 = abapi.Ability(name = "conv1proc2", id = ID.newID,
    preCondition = prop(allVars, s"${c1p2State.name} == 1 and not ${c1p2Run.name} and not ${c1p2Sensor.name} and ${c1p1State.name} == 1",
      List(s"${c1p2Run.name} := true", s"${c1p2Dir.name} := false")), //Not sure if we are supposed to set direction here or somewhere else, now it moves right
    postCondition = prop(allVars, s"${c1p2State.name} == 3", List(s"${c1p2Run.name} := false")),
    started = prop(allVars, s"${c1p2State.name} == 2", List())
  )
  

  val robot1to1 = abapi.Ability(name = "robot1to1", id = ID.newID,
    preCondition = prop(allVars, s"${robot1State.name} == 1 and not ${robot1Run.name} and ((${robot1gripping.name} and  ${c1p1State.name} == 1 " +
      s"and not ${c1p1Sensor.name} and not ${c1p2Sensor.name}) or (not ${robot1gripping.name} and ${c1p1State.name} == 1 and ${c1p1Sensor.name}))",
      List(s"${robot1Run.name} := true", s"${robot1Target.name} := 1")),
    postCondition = prop(allVars, s"${robot1State.name} == 3", List(s"${robot1Run.name} := false")),
    started = prop(allVars, s"${robot1State.name} == 2", List())
  )
  println(robot1to1);
  val allAbilities = List(a1, a2, feeder, conv1proc1, conv1proc2,robot1to1)
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

    PropositionCondition(cRes, aRes)
  }


  val plcPath = "|var|CODESYS Control for Raspberry Pi SL.Application."
  def makeTheOPCVariables(xs: List[Thing], prefix: String = "") = {
    xs.map(t => t -> (plcPath + prefix + t.name))
  }
}
