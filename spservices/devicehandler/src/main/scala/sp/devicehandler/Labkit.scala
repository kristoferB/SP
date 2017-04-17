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
  // Conv 1
  val  c1p1Run          = Thing("c1p1Run")
  val  c1p1Dir          = Thing("c1p1Dir")
  val  c1p1State        = Thing("c1p1State")
  val  c1p1Sensor       = Thing("c1p1Sensor")

  val  c1p2Run          = Thing("c1p2Run")
  val  c1p2Dir          = Thing("c1p2Dir")
  val  c1p2State        = Thing("c1p2State")
  val  c1p2Sensor       = Thing("c1p2Sensor")
  // Conv 2
  val  c2p1Run          = Thing("c2p1Run")
  val  c2p1Dir          = Thing("c2p1Dir")
  val  c2p1State        = Thing("c2p1State")
  val  c2p1Sensor       = Thing("c2p1Sensor")
  // Conv 3
  val  c3p1Run          = Thing("c3p1Run")
  val  c3p1Dir          = Thing("c3p1Dir")
  val  c3p1State        = Thing("c3p1State")
  val  c3p1Sensor       = Thing("c3p1Sensor")

  val  c3p2Run          = Thing("c3p2Run")
  val  c3p2Dir          = Thing("c3p2Dir")
  val  c3p2State        = Thing("c3p2State")
  val  c3p2Sensor       = Thing("c3p2Sensor")

  val  c3p3Run          = Thing("c3p3Run")
  val  c3p3Dir          = Thing("c3p3Dir")
  val  c3p3State        = Thing("c3p3State")
  val  c3p3Sensor       = Thing("c3p3Sensor")
  // Conv 4
  val  c4p1Run          = Thing("c4p1Run")
  val  c4p1Dir          = Thing("c4p1Dir")
  val  c4p1State        = Thing("c4p1State")
  val  c4p1Sensor       = Thing("c4p1Sensor")

  val  c4p2Run          = Thing("c4p2Run")
  val  c4p2Dir          = Thing("c4p2Dir")
  val  c4p2State        = Thing("c4p2State")
  val  c4p2Sensor       = Thing("c4p2Sensor")

  val  c4p3Run          = Thing("c4p3Run")
  val  c4p3Dir          = Thing("c4p3Dir")
  val  c4p3State        = Thing("c4p3State")
  val  c4p3Sensor       = Thing("c4p3Sensor")

  val  robot1Run        = Thing("robot1Run")
  val  robot1Target     = Thing("robot1Target")
  val  robot1gripping   = Thing("robot1gripping")
  val  robot1State      = Thing("robot1State")
  val  robot1ResetRun   = Thing("robot1ResetRun")
  val  robot1ResetState = Thing("robot1ResetState")

  val  robot2Run        = Thing("robot2Run")
  val  robot2Target     = Thing("robot2Target")
  val  robot2gripping   = Thing("robot2gripping")
  val  robot2State      = Thing("robot2State")
  val  robot2ResetRun   = Thing("robot2ResetRun")
  val  robot2ResetState = Thing("robot2ResetState")

  // resources
  val testingResource = Thing("testingResource")


  val allVars = List(testingIn, testingInInt, testingOut, testingOutInt, testingOutMirror, feedRun, feedSensor, feedState, c1p1Run, c1p1Dir,
    c1p1State, c1p1Sensor, c1p2Run, c1p2Dir, c1p2State, c1p2Sensor, c2p1Run, c2p1Dir, c2p1State, c2p1Sensor, c3p1Run, c3p1Dir, c3p1State, c3p1Sensor, c3p2Run
      , c3p2Dir, c3p2State, c3p2Sensor, c3p3Run, c3p3Dir, c3p3State, c3p3Sensor, c4p1Run, c4p1Dir, c4p1State, c4p1Sensor,
    c4p2Run, c4p2Dir, c4p2State, c4p2Sensor, c4p3Run, c4p3Dir, c4p3State, c4p3Sensor, robot1Run, robot1Target, robot1State, robot1ResetRun, robot1ResetState,
    robot1gripping, robot2Run, robot2Target,robot2gripping, robot2State, robot2ResetRun, robot2ResetState )

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
 // Conv 1
  val conv1proc1 = abapi.Ability(name = "conv1proc1", id = ID.newID,
    preCondition = prop(allVars, s"${c1p1State.name} == 1 and not ${c1p1Run.name} and not ${c1p1Sensor.name} and ${c1p2State.name} == 1",
      List(s"${c1p1Run.name} := true", s"${c1p1Dir.name} := true")), //Not sure if we are supposed to set direction here or somewhere else, now it moves right
    postCondition = prop(allVars, s"${c1p1State.name} == 3", List(s"${c1p1Run.name} := false", s"${c1p1Dir.name} := false)")),
    started = prop(allVars, s"${c1p1State.name} == 2", List())
  )
  // Problem med kommunikationen mellan Codesys och SP för c1p2Sensor
  val conv1proc2 = abapi.Ability(name = "conv1proc2", id = ID.newID,
    preCondition = prop(allVars, s"${c1p2Sensor.name}",
     // s"${c1p2State.name} == 1 and not ${c1p2Sensor.name} and ${c1p1State.name} == 1",
      List(s"${c1p2Run.name} := true", s"${c1p2Dir.name} := false")),
    postCondition = prop(allVars, s"${c1p2State.name} == 3", List(s"${c1p2Run.name} := false")),
    started = prop(allVars, s"${c1p2State.name} == 2", List())
  )
  // Conv 2
  val conv2proc1 = abapi.Ability(name = "conv2proc1", id = ID.newID,
    preCondition = prop(allVars, s"${c2p1State.name} == 1 and not ${c2p1Sensor.name}",// maybe a not robot to 5
      List(s"${c2p1Run.name} := true", s"${c2p1Dir.name} := true")),
    postCondition = prop(allVars, s"${c2p1State.name} == 3", List(s"${c2p1Run.name} := false", s"${c2p1Dir.name} := false)")),
    started = prop(allVars, s"${c2p1State.name} == 2", List())
  )
 // Conv 3
 val conv3proc1right = abapi.Ability(name = "conv3proc1right", id = ID.newID,
    preCondition = prop(allVars, s"${c3p1State.name} == 1 and ${c3p2State.name} == 1 and ${c3p3State.name} == 1 and not ${c3p1Sensor.name}",
      List(s"${c3p1Run.name} := true", s"${c3p1Dir.name} := true")),
    postCondition = prop(allVars, s"${c3p1State.name} == 3", List(s"${c3p1Dir.name} := false)")),
    started = prop(allVars, s"${c3p1State.name} == 2", List())
  )
  val conv3proc1left = abapi.Ability(name = "conv3proc1left", id = ID.newID,
    preCondition = prop(allVars, s"${c3p1State.name} == 1 and ${c3p2State.name} == 1 and ${c3p3State.name} == 1 and not ${c3p1Sensor.name}",
      List(s"${c3p1Run.name} := true", s"${c3p1Dir.name} := false")),
    postCondition = prop(allVars, s"${c3p1State.name} == 3", List(s"${c3p1Run.name} := false")),
    started = prop(allVars, s"${c3p1State.name} == 2", List())
  )
  val conv3proc2right = abapi.Ability(name = "conv3proc2right", id = ID.newID,
    preCondition = prop(allVars, s"${c3p2State.name} == 1 and ${c3p1State.name} == 1 and ${c3p3State.name} == 1 and not ${c3p2Sensor.name}",
      List(s"${c3p2Run.name} := true", s"${c3p2Dir.name} := true")),
    postCondition = prop(allVars, s"${c3p2State.name} == 3", List(s"${c3p2Run.name} := false", s"${c3p2Dir.name} := false)")),
    started = prop(allVars, s"${c3p2State.name} == 2", List())
  )
  val conv3proc2left = abapi.Ability(name = "conv3proc2left", id = ID.newID,
    preCondition = prop(allVars, s"${c3p2State.name} == 1 and ${c3p1State.name} == 1 and ${c3p3State.name} == 1 and not ${c3p2Sensor.name}",
      List(s"${c3p2Run.name} := true", s"${c3p2Dir.name} := false")),
    postCondition = prop(allVars, s"${c3p2State.name} == 3", List(s"${c3p2Run.name} := false")),
    started = prop(allVars, s"${c3p2State.name} == 2", List())
  )
  val conv3proc3 = abapi.Ability(name = "conv3proc3", id = ID.newID,
    preCondition = prop(allVars, s"${c3p3State.name} == 1 and not ${c3p3Sensor.name} and ${c3p1State.name} == 1 and ${c3p2State.name} == 1",
      List(s"${c3p3Run.name} := true", s"${c3p3Dir.name} := true")),
    postCondition = prop(allVars, s"${c3p3State.name} == 3", List(s"${c3p3Dir.name} := false)", s"${c3p3Run.name} := false")),
    started = prop(allVars, s"${c3p3State.name} == 2", List())
  )

  val conv3DirSet = abapi.Ability(name = "conv3DirSet", id = ID.newID,
    preCondition = prop(allVars, s"1 == 1", List(s"${c3p1Dir.name} := false")),
    postCondition = prop(allVars, s"${c3p1Dir.name} == false", List()),
    started = prop(allVars, s"${c3p1Dir.name} == false", List())
  )
  // Conv 4

  val conv4proc1right = abapi.Ability(name = "conv4proc1right", id = ID.newID,
    preCondition = prop(allVars, s"${c4p1State.name} == 1 and ${c4p2State.name} == 1 and ${c4p3State.name} == 1 and not ${c4p1Sensor.name}",
      List(s"${c4p1Run.name} := true", s"${c4p1Dir.name} := true")),
    started = prop(allVars, s"${c4p1State.name} == 2", List()),
    postCondition = prop(allVars, s"${c4p1State.name} == 3", List(s"${c4p1Run.name} := false)", s"${c4p1Dir.name} := false)"))
  )

  val conv4proc1left = abapi.Ability(name = "conv4proc1left", id = ID.newID,
    preCondition = prop(allVars, s"${c4p1State.name} == 1 and ${c4p2State.name} == 1 and ${c4p3State.name} == 1 and not ${c4p1Sensor.name}",
      List(s"${c4p1Run.name} := true", s"${c4p1Dir.name} := false")),
    started = prop(allVars, s"${c4p1State.name} == 2", List()),
    postCondition = prop(allVars, s"${c4p1State.name} == 3", List(s"${c4p1Run.name} := false", s"${c4p1Dir.name} := false)"))
  )

  val conv4proc2right = abapi.Ability(name = "conv4proc2right", id = ID.newID,
    preCondition = prop(allVars, s"${c4p2State.name} == 1 and ${c4p1State.name} == 1 and ${c4p3State.name} == 1 and not ${c4p2Sensor.name}",
      List(s"${c4p2Run.name} := true", s"${c4p2Dir.name} := true")),
    postCondition = prop(allVars, s"${c4p2State.name} == 3", List(s"${c4p2Run.name} := false", s"${c4p2Dir.name} := false)")),
    started = prop(allVars, s"${c4p2State.name} == 2", List())
  )

  val conv4proc2left = abapi.Ability(name = "conv4proc2left", id = ID.newID,
    preCondition = prop(allVars, s"${c4p2State.name} == 1 and ${c4p1State.name} == 1 and ${c4p3State.name} == 1 and not ${c4p2Sensor.name}",
      List(s"${c4p2Run.name} := true", s"${c4p2Dir.name} := false")),
    postCondition = prop(allVars, s"${c4p2State.name} == 3", List(s"${c4p2Run.name} := false", s"${c4p2Dir.name} := false)")),
    started = prop(allVars, s"${c4p2State.name} == 2", List())
  )

  val conv4proc3right = abapi.Ability(name = "conv4proc3right", id = ID.newID,
    preCondition = prop(allVars, s"${c4p3State.name} == 1 and ${c4p1State.name} == 1 and ${c4p2State.name} == 1 and not ${c4p3Sensor.name}",
      List(s"${c4p3Run.name} := true", s"${c4p3Dir.name} := true")),
    postCondition = prop(allVars, s"${c4p3State.name} == 3", List(s"${c4p3Run.name} := false", s"${c4p3Dir.name} := false)")),
    started = prop(allVars, s"${c4p3State.name} == 2", List())
  )

  val conv4proc3left = abapi.Ability(name = "conv4proc3left", id = ID.newID,
    preCondition = prop(allVars, s"${c4p3State.name} == 1 and ${c4p1State.name} == 1 and ${c4p2State.name} == 1 and not ${c4p3Sensor.name}",
      List(s"${c4p3Run.name} := true", s"${c4p3Dir.name} := false")),
    postCondition = prop(allVars, s"${c4p3State.name} == 3", List(s"${c4p3Run.name} := false", s"${c4p3Dir.name} := false)")),
    started = prop(allVars, s"${c4p3State.name} == 2", List())
  )

  // Robot 1

  val robot1to1pick = abapi.Ability(name = "robot1to1pick", id = ID.newID,
    preCondition = prop(allVars, s"${robot1State.name} == 1 and not ${robot1Run.name} and not ${robot1gripping.name} and  ${c1p1State.name} == 1 " +
      s"and ${c1p1Sensor.name} and not ${c1p2Sensor.name} ",
      List(s"${robot1Run.name} := true", s"${robot1Target.name} := 5")),
    postCondition = prop(allVars, s"${robot1State.name} == 3", List(s"${robot1Run.name} := false")),
    started = prop(allVars, s"${robot1State.name} == 2", List())
  )

  val robot1to2put = abapi.Ability(name = "robot1to2put", id = ID.newID,
    preCondition = prop(allVars, s"${robot1State.name} == 1 and not ${robot1Run.name} and ${robot1gripping.name} and ${c2p1State.name} == 1" +
      s"and not ${c2p1Run.name} and ${conv2proc1.name} == 1",
      List(s"${robot1Run.name} := true", s"${robot1Target.name} := 1")),
    postCondition = prop(allVars, s"${robot1State.name} == 3", List(s"${robot1Run.name} := false")),
    started = prop(allVars, s"${robot1State.name} == 2", List())
  )

  val robot1to1put = abapi.Ability(name = "robot1to1put", id = ID.newID,
    preCondition = prop(allVars, s"${robot1State.name} == 1 and not ${robot1Run.name} and ${robot1gripping.name} and ${c1p1State.name} == 1" +
      s"and not ${c1p1Run.name} and not ${c1p1Sensor.name} and not ${c1p2Sensor.name} and ${c1p2State.name} == 1",
      List(s"${robot1Run.name} := true", s"${robot1Target.name} := 5")),
    postCondition = prop(allVars, s"${robot1State.name} == 3", List(s"${robot1Run.name} := false")),
    started = prop(allVars, s"${robot1State.name} == 2", List())
  )

  val robot1toFeedCylPick = abapi.Ability(name = "robot1toFeedCylPick ", id = ID.newID,
    preCondition = prop(allVars, s"${robot1State.name} == 1 and not ${robot1Run.name} and not ${robot1gripping.name} and ${feedSensor.name}",
      List(s"${robot1Run.name} := true", s"${robot1Target.name} := 3")),
    postCondition = prop(allVars, s"${robot1State.name} == 3", List(s"${robot1Run.name} := false")),
    started = prop(allVars, s"${robot1State.name} == 2", List())
  )


  // Robot 2

  val robot2to3put = abapi.Ability(name = "robot2to3put", id = ID.newID,
    preCondition = prop(allVars, s"${robot2State.name} == 1 and not ${robot2Run.name} and ${robot2gripping.name} and ${c3p1State.name} == 1" +
      s"and ${c3p2State.name} == 1 and ${c3p3State.name} == 1 and not ${c3p1Sensor.name} and not ${c3p2Sensor.name} and not ${c3p3Sensor.name} ",
      List(s"${robot2Run.name} := true", s"${robot2Target.name} := 3")),
    postCondition = prop(allVars, s"${robot2State.name} == 3", List(s"${robot2Run.name} := false")),
    started = prop(allVars, s"${robot2State.name} == 2", List())
  )

  val robot2to4put = abapi.Ability(name = "robot2to4put", id = ID.newID,
    preCondition = prop(allVars, s"${robot2State.name} == 1 and not ${robot2Run.name} and ${robot2gripping.name} and ${c4p1State.name} == 1" +
      s"and ${c4p2State.name} == 1 and ${c4p3State.name} == 1 and not ${c4p1Sensor.name} and not ${c4p2Sensor.name} and not ${c4p3Sensor.name} ",
      List(s"${robot2Run.name} := true", s"${robot2Target.name} := 1")),
    postCondition = prop(allVars, s"${robot2State.name} == 3", List(s"${robot2Run.name} := false")),
    started = prop(allVars, s"${robot2State.name} == 2", List())
  )

  val robot2to4pick = abapi.Ability(name = "robot2to4pick", id = ID.newID,
    preCondition = prop(allVars, s"${robot2State.name} == 1 and not ${robot2Run.name} and not ${robot2gripping.name} and ${c4p1State.name} == 1" +
      s"and not ${c4p1Run.name} and ${c4p1Sensor.name} ",
      List(s"${robot2Run.name} := true", s"${robot2Target.name} := 1")),
    postCondition = prop(allVars, s"${robot2State.name} == 3", List(s"${robot2Run.name} := false")),
    started = prop(allVars, s"${robot2State.name} == 2", List())
  )

  val robot2to2pick = abapi.Ability(name = "robot2to2pick", id = ID.newID,
    preCondition = prop(allVars, s"${robot2State.name} == 1 and not ${robot2Run.name} and not ${robot2gripping.name} and  ${c2p1State.name} == 1 " +
      s"and ${c2p1Sensor.name} ",
      List(s"${robot2Run.name} := true", s"${robot2Target.name} := 5")),
    postCondition = prop(allVars, s"${robot2State.name} == 3", List(s"${robot2Run.name} := false")),
    started = prop(allVars, s"${robot2State.name} == 2", List())
  )
  val allAbilities = List(a1, a2, feeder, conv1proc1, conv1proc2,conv2proc1, conv3proc1left, conv3proc1right, conv3proc2left, conv3proc2right,
    conv3proc3,conv4proc1right, conv4proc1left, conv4proc2right, conv4proc2left, conv4proc3right, conv4proc3left, robot1to1put, robot1to1pick,
    robot1to2put, robot1toFeedCylPick, robot2to2pick, robot2to3put, robot2to4pick, robot2to4put, conv3DirSet)

  println(allAbilities)



  // setupj6 driver
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
