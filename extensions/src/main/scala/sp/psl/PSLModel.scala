package sp.psl

import akka.actor._
import sp.domain.logic.{PropositionParser, ActionParser, IDAbleLogic}
import sp.psl.runnerService.RunnerService
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._
import sp.domain.Operation
import com.typesafe.config._



object PSLModel extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "models",
      "description" -> "Creates items describing the PSL system"
    )
  )
  val transformation: List[TransformValue[_]] = List()
  def props = Props(classOf[PSLModel])
  //def props(eventHandler: ActorRef, serviceHandler: ActorRef, runnerService: String) =
    //ServiceLauncher.props(Props(classOf[PSLModel], eventHandler, serviceHandler, runnerService))
}

case class AbilityParameter(id: ID, value: SPValue)
case class AbilityStructure(id: ID,
                            parameters: List[AbilityParameter] = List(),
                            state: State = State(Map()))





class PSLModel extends Actor with ServiceSupport with ThePSLModel {
  import context.dispatcher

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get


      val items = getCompleteModel

      replyTo ! Response(items, SPAttributes("info"->"Items created from PSLModel service"), rnr.req.service, rnr.req.reqID)
    }
  }
}

case class DBConnection(name: String, valueType: String, db: Int, byte: Int = 0, bit: Int = 0, intMap: Map[String, String] = Map(), id: ID = ID.newID, busAddress: String = "")

trait ThePSLModel extends Resources with DBConnector{
  def getCompleteModel: List[IDAble] = {

    val items = getResources
    val itemMap = items.map(x => x.name -> x.id).toMap

    // This info will later on be filled by a service on the bus
    val connectionList = getDBMap(itemMap)
    val connection = SPSpec("PLCConnection", SPAttributes(
      "connection"->connectionList,
      "specification"-> "PLCConnection"
    ))

    // In the future, the matching should be on an ability type structure
    // Also the sequence now created in OperatorService should be mad based on
    // conditions here
    val ops  = makeMeOperationTypes(List(
      OpTowerTypeDef(
        name ="PickPlatesR2",
        ability = itemMap("R2.pickAtPos"),
        parameters = List(itemMap("R2.pickAtPos.pos") -> 0)
      ),
      OpTowerTypeDef(
        name ="PlaceElevatorR2",
        ability = itemMap("R2.homeTableToElevatorStn3"),
        parameters = List()
      ),
      OpTowerTypeDef(
        name ="PlaceTableR2",
        ability = itemMap("R2.deliverTower"),
        parameters = List()
      ),
      OpTowerTypeDef(
        name ="PickBlocksR4",
        ability = itemMap("R4.pickBlock"),
        parameters = List(itemMap("R4.pickBlock.pos") -> 0)
      ),
      OpTowerTypeDef(
        name ="PickBlocksR5",
        ability = itemMap("R5.pickBlock"),
        parameters  = List(itemMap("R5.pickBlock.pos") -> 0)
      ),
      OpTowerTypeDef(
        name ="PlaceBlocksR4",
        ability = itemMap("R4.placeBlock"),
        parameters  = List(itemMap("R4.placeBlock.pos") -> 0)

      ),
      OpTowerTypeDef(
        name ="PlaceBlocksR5",
        ability = itemMap("R5.placeBlock"),
        parameters = List(itemMap("R5.placeBlock.pos") -> 0)
      ),
      OpTowerTypeDef(
        name ="LoadFixtureOp",
        ability = itemMap("Operator.loadFixture"),
        parameters  = List(itemMap("Operator.loadFixture.brickPositions") -> 0)

      ),
      OpTowerTypeDef(
        name ="FixtureToRobots",
        ability = itemMap("Flexlink.fixtureToRobot"),
        parameters = List(itemMap("Flexlink.fixtureToRobot.pos") -> 0)
      ),
      OpTowerTypeDef(
        name ="FixtureToOperator",
        ability = itemMap("Flexlink.fixtureToOperator"),
        parameters = List(itemMap("Flexlink.fixtureToOperator.no") -> 0)
      )

    ))

    items ++ ops :+ connection
  }


}

trait Resources extends ModelMaking {
  def getResources = {
    // Make these sub resources to Flexlink
    val h2 = makeResource (
      name = "H2",
      state = List(),
      abilities = List(
        AbilityDefinition(name = "up"),
        AbilityDefinition("down")
      )
    )
    val h3 = makeResource (
      name = "H3",
      state = List(),
      abilities = List(
        AbilityDefinition(name = "up"),
        AbilityDefinition("down")
      )
    )

    //skicka klossplatta till operatör
    val flexlink = makeResource (
      name = "Flexlink",
      state = List("running"),
      abilities = List(
        AbilityDefinition(name = "fixtureToRobot", parameters = List(sOrP("pos", 0))),
        AbilityDefinition("fixtureToOperator", List(sOrP("no", 2)))
      )

    )

    val R5 = makeResource (
      name = "R5",
      state = List(),
      abilities = List(
        AbilityDefinition("pickBlock",  List(sOrP("pos", 0))),
        AbilityDefinition("placeBlock", List(sOrP("pos", 0))),
        AbilityDefinition("toHome"),
        AbilityDefinition("toDodge")
      )
    )

    val R4 = makeResource (
      name = "R4",
      state = List(),
      abilities = List(
        AbilityDefinition("pickBlock",  List(sOrP("pos", 0))),
        AbilityDefinition("placeBlock", List(sOrP("pos", 0))),
        AbilityDefinition("toHome"),
        AbilityDefinition("toDodge")
      )
    )

    val R2 = makeResource (
      name = "R2",
      state = List(),
      abilities = List(
        AbilityDefinition("pickAtPos",  List(sOrP("pos", 0))),
        AbilityDefinition("placeAtPos", List(sOrP("pos", 0))),
        AbilityDefinition("homeTableToElevatorStn3"),
        AbilityDefinition("elevatorStn2ToHomeTable"),
        AbilityDefinition("deliverTower"),
        AbilityDefinition("pickBuildPlate")
      )
    )


    val operator = makeResource (
      name = "Operator",
      state = List(),
      abilities = List(
        AbilityDefinition("loadFixture"),
        AbilityDefinition("brickPositions")
      )
    )


    val root = HierarchyRoot("Resources", List(flexlink._1, operator._1, R2._1, R4._1, R5._1, h2._1, h3._1))
    h2._2 ++ h3._2 ++ flexlink._2 ++ operator._2 ++ R5._2 ++ R4._2 ++ R2._2 :+ root

  }
}

trait DBConnector {
  def db(items: Map[String, ID], name: String, valueType: String, db:Int, byte: Int, bit: Int, intMap: Map[Int, String] = Map(), busAddress: String = "") = {
    items.get(name).map(id => DBConnection(name, valueType, db, byte, bit, intMap.map{case (k,v) => k.toString->v}, id, busAddress))
  }


  def getDBMap(itemMap: Map[String, ID]) = {
    val stateMap = Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")

    List(
      db(itemMap, "H2.up.run", "bool", 135, 0, 0),
      db(itemMap, "H2.down.run", "bool", 135, 0, 1),
      db(itemMap, "H2.up.mode", "int", 135, 2, 0, stateMap),
      db(itemMap, "H2.down.mode", "int", 135, 4, 0, stateMap),

      db(itemMap, "H3.up.run", "bool", 140, 0, 0),
      db(itemMap, "H3.down.run", "bool", 140, 0, 1),
      db(itemMap, "H3.up.mode", "int", 140, 2, 0, stateMap),
      db(itemMap, "H3.down.mode", "int", 140, 4, 0, stateMap),

      db(itemMap, "Flexlink.fixtureToOperator.run", "bool", 139, 0, 0),
      db(itemMap, "Flexlink.fixtureToRobot.run", "bool", 139, 0, 1),
      db(itemMap, "Flexlink.fixtureToOperator.mode", "int", 139, 2, 0, stateMap),
      db(itemMap, "Flexlink.fixtureToRobot.mode", "int", 139, 4, 0, stateMap),
      db(itemMap, "Flexlink.fixtureToOperator.no", "int", 139, 6, 0),
      db(itemMap, "Flexlink.fixtureToRobot.pos", "int", 139, 10, 0),
      //kolla adress // db(itemMap, "Flexlink.running", "int", 139, 2, 0, stateMap),

      db(itemMap, "R5.pickBlock.run", "bool", 132, 0, 0),
      db(itemMap, "R5.pickBlock.mode", "int", 132, 2, 0, stateMap),
      db(itemMap, "R5.pickBlock.pos", "int", 132, 10, 0),
      db(itemMap, "R5.placeBlock.run", "bool", 132, 0, 1),
      db(itemMap, "R5.placeBlock.mode", "int", 132, 4, 0, stateMap),
      db(itemMap, "R5.placeBlock.pos", "int", 132, 10, 0),
      db(itemMap, "R5.toHome.run", "bool", 132, 0, 2),
      db(itemMap, "R5.toHome.mode", "int", 132, 6, 0, stateMap),
      db(itemMap, "R5.toDodge.run", "bool", 132, 0, 3),
      db(itemMap, "R5.toDodge.mode", "int", 132, 8, 0, stateMap),

      db(itemMap, "R4.pickBlock.run", "bool", 128, 0, 0),
      db(itemMap, "R4.pickBlock.pos", "int", 128, 10, 0),
      db(itemMap, "R4.placeBlock.pos", "int", 128, 10, 0),
      db(itemMap, "R4.pickBlock.mode", "int", 128, 2, 0, stateMap),
      db(itemMap, "R4.placeBlock.run", "bool", 128, 0, 1),
      db(itemMap, "R4.placeBlock.mode", "int", 128, 4, 0, stateMap),
      db(itemMap, "R4.toHome.run", "bool", 128, 0, 2),
      db(itemMap, "R4.toHome.mode", "int", 128, 6, 0, stateMap),
      db(itemMap, "R4.toDodge.run", "bool", 128, 0, 3),
      db(itemMap, "R4.toDodge.mode", "int", 128, 8, 0, stateMap),

      db(itemMap, "R2.elevatorStn2ToHomeTable.run", "bool",126,0,0),
      db(itemMap, "R2.elevatorStn2ToHomeTable.mode", "int", 126, 2, 0, stateMap),
      db(itemMap, "R2.homeTableToElevatorStn3.run", "bool",126,0,2),
      db(itemMap, "R2.homeTableToElevatorStn3.mode", "int", 126, 6, 0, stateMap),
      db(itemMap, "R2.placeAtPos.run", "bool",126,0,4),
      db(itemMap, "R2.placeAtPos.pos", "bool",126,18,0),
      db(itemMap, "R2.placeAtPos.mode", "int", 126, 10, 0, stateMap),
      db(itemMap, "R2.pickAtPos.run", "bool",126,0,5),
      db(itemMap, "R2.pickAtPos.pos", "int",126,18,0),
      db(itemMap, "R2.pickAtPos.mode", "int", 126, 12, 0, stateMap),
      db(itemMap, "R2.deliverTower.run", "bool",126,0,6),
      db(itemMap, "R2.deliverTower.mode", "int", 126, 14, 0, stateMap),
      db(itemMap, "R2.pickBuildPlate.run", "bool",126,0,7),
      db(itemMap, "R2.pickBuildPlate.mode", "int", 126, 16, 0, stateMap),
      //db(itemMap, "R2.homeTableToHomeBP.run", "bool",126,0,1),
      //db(itemMap, "R2.homeBPToHomeTable.run", "bool",126,0,3),
      //db(itemMap, "R2.homeTableToHomeBP.mode", "int", 126, 4, 0, stateMap),
      //db(itemMap, "R2.homeBPToHomeTable.mode", "int", 126, 8, 0, stateMap),

      db(itemMap, "Operator.loadFixture.run", "bool", 0, 0, 0, Map(), "operatorInstructions.run"),
      db(itemMap, "Operator.loadFixture.mode", "int", 0, 0, 0, stateMap, "operatorInstructions.mode"),
      db(itemMap, "Operator.loadFixture.brickPositions", "bool", 0, 0, 0, Map(), "operatorInstructions.brickPositions")

    ).flatten

  }
}

// Only used for modeling here
case class sOrP(name: String, value: SPValue)
case class AbilityDefinition(name: String, parameters: List[sOrP]=List(), states: List[sOrP]=List(), abilityType: SPValue = "")

trait ModelMaking {
  def makeResource(name: String, state: List[String], abilities: List[AbilityDefinition]) = {
    val t = Thing(name)
    val stateVars = Thing(s"$name.mode", SPAttributes("variableType"->"state")) :: state.map(x => Thing(s"$name.$x", SPAttributes("variableType"->"state")))
    val ab = abilities.map{as =>

      val abilityName = name +"."+as.name
      val o = Operation(abilityName, List(), SPAttributes("operationType"->"ability", "abilityType"->as.abilityType))

      val parameters = (as.parameters ++ List(sOrP("run", false))).map{ p =>
        Thing(abilityName+"."+p.name, SPAttributes("variableType"->"parameter", "init"->p.value))
      }
      val states = (as.states ++ List(sOrP("mode", 0))).map{ p =>
        Thing(abilityName+"."+p.name, SPAttributes("variableType"->"state", "init"->p.value))
      }

      (o, parameters ++ states)

    }
    val abHir = ab.map{case (op, para) => HierarchyNode(op.id, para.map(p => HierarchyNode(p.id)))}
    val hier = HierarchyNode(t.id, stateVars.map(x => HierarchyNode(x.id)) ++ abHir)
    val temp: List[IDAble] = t :: stateVars ++ ab.map(_._1) ++ ab.flatMap(_._2)

    (hier, temp)
  }


  case class OpTowerTypeDef(name: String, ability: ID, parameters: List[(ID, Int)])
  def makeMeOperationTypes(xs: List[OpTowerTypeDef]): List[IDAble] = {
    xs.map{x =>
      val ab = AbilityStructure(x.ability, x.parameters.map(p => AbilityParameter(p._1, p._2)))
      Operation(x.name, List(), SPAttributes(
        "operationType"->"operation",
        "ability"-> ab
      ))

    }
  }


}







