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
case class AbilityStructure(id: ID, parameters: List[AbilityParameter])

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

case class DBConnection(name: String, valueType: String, db: Int, byte: Int = 0, bit: Int = 0, intMap: Map[String, String] = Map(), id: ID = ID.newID)

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

    val ops  = makeMeOperationTypes(List(
      OpTowerTypeDef(
        name ="PickBlocksR4",
        op = "tower",
        pick = List(1,2),
        place = List(),
        ability = itemMap("R4.pickBlock"),
        parameter = itemMap("R4.pickBlock.pos")
      ),
      OpTowerTypeDef(
        name ="PickBlocksR5",
        op = "tower",
        pick = List(3,4),
        place = List(),
        ability = itemMap("R5.pickBlock"),
        parameter  = itemMap("R5.pickBlock.pos")
      ),
      OpTowerTypeDef(
        name ="PlaceBlocksR4",
        op = "tower",
        pick = List(),
        place = List(1,2),
        ability = itemMap("R4.placeBlock"),
        parameter  = itemMap("R4.placeBlock.pos")

      ),
      OpTowerTypeDef(
        name ="PlaceBlocksR5",
        op = "tower",
        pick = List(),
        place = List(3,4),
        ability = itemMap("R5.placeBlock"),
        parameter = itemMap("R5.placeBlock.pos")
      ),
      OpTowerTypeDef(
        name ="LoadFixtureOp",
        op = "load",
        pick = List(),
        place = List(),
        ability = itemMap("Operator.loadFixture"),
        parameter  = itemMap("Operator.loadFixture.brickPositions")

      ),
      OpTowerTypeDef(
        name ="FixtureToRobots",
        op = "fixture",
        pick = List(),
        place = List(),
        ability = itemMap("flexlink.fixtureToRobot"),
        parameter = itemMap("flexlink.fixtureToRobot.pos")
      )

    ))

    items ++ ops :+ connection
  }


}

trait Resources extends ModelMaking {
  def getResources = {
    // Make these sub resources to Flexlink
    val h2 = makeResource (
      name = "h2",
      state = List(),
      abilities = List("up"->List(), "down"->List())
    )
    val h3 = makeResource (
      name = "h3",
      state = List(),
      abilities = List("up"->List(), "down"->List())
    )

    //skicka klossplatta till operatör
    val flexlink = makeResource (
      name = "flexlink",
      state = List("running"),
      abilities = List("fixtureToRobot"-> List("pos"))
    )

    val R5 = makeResource (
      name = "R5",
      state = List(),
      abilities = List("pickBlock"->List("pos"), "placeBlock"->List("pos"),
        "toHome"->List(), "toDodge"->List())
    )

    val R4 = makeResource (
      name = "R4",
      state = List(),
      abilities = List("pickBlock"->List("pos"), "placeBlock"->List("pos"),
        "toHome"->List(), "toDodge"->List())
    )

    val R2 = makeResource (
      name = "R2",
      state = List(),
      abilities = List("elevatorStn2ToHomeTable"->List(), "homeTableToHomeBP" ->List(),
        "homeTableToElevatorStn3"->List(), "homeBPToHomeTable"->List(),
        "placeAtPos"->List("pos"), "pickAtPos"->List("pos"))
    )

    val operator = makeResource (
      name = "Operator",
      state = List(),
      abilities = List("loadFixture"->List("brickPositions"))
    )

    val root = HierarchyRoot("Resources", List(flexlink._1, operator._1, R2._1, R4._1, R5._1, h2._1, h3._1))
    h2._2 ++ h3._2 ++ flexlink._2 ++ operator._2 ++ R5._2 ++ R4._2 ++ R2._2 :+ root

  }
}

trait DBConnector {
  def db(items: Map[String, ID], name: String, valueType: String, db:Int, byte: Int, bit: Int, intMap: Map[Int, String] = Map()) = {
    items.get(name).map(id => DBConnection(name, valueType, db, byte, bit, intMap.map{case (k,v) => k.toString->v}, id))
  }

  def getDBMap(itemMap: Map[String, ID]) = {
    val stateMap = Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")

    List(
      db(itemMap, "h2.up.run", "bool", 135, 0, 0),
      db(itemMap, "h2.down.run", "bool", 135, 0, 1),
      db(itemMap, "h2.up.mode", "int", 135, 2, 0, stateMap),
      db(itemMap, "h2.down.mode", "int", 135, 4, 0, stateMap),

      db(itemMap, "h3.up.run", "bool", 140, 0, 0),
      db(itemMap, "h3.down.run", "bool", 140, 0, 1),
      db(itemMap, "h3.up.mode", "int", 140, 2, 0, stateMap),
      db(itemMap, "h3.down.mode", "int", 140, 4, 0, stateMap),

      db(itemMap, "flexlink.sendFixtureToRobot.run", "bool", 139, 0, 0),
      db(itemMap, "flexlink.sendFixtureToRobot.mode", "int", 139, 2, 0, stateMap),
      //kolla adress // db(itemMap, "flexlink.running", "int", 139, 2, 0, stateMap),

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
      db(itemMap, "R2.homeTableToHomeBP.run", "bool",126,0,1),
      db(itemMap, "R2.homeTableToElevatorStn3.run", "bool",126,0,2),
      db(itemMap, "R2.homeBPToHomeTable.run", "bool",126,0,3),
      db(itemMap, "R2.placeAtPos.run", "bool",126,0,4),
      db(itemMap, "R2.pickAtPos.run", "bool",126,0,5),
      db(itemMap, "R2.elevatorStn2ToHomeTable.mode", "int", 126, 2, 0, stateMap),
      db(itemMap, "R2.homeTableToHomeBP.mode", "int", 126, 4, 0, stateMap),
      db(itemMap, "R2.homeTableToElevatorStn3.mode", "int", 126, 6, 0, stateMap),
      db(itemMap, "R2.homeBPToHomeTable.mode", "int", 126, 8, 0, stateMap),
      db(itemMap, "R2.placeAtPos.run", "int", 126, 10, 0, stateMap),
      db(itemMap, "R2.pickAtPos.run", "int", 126, 12, 0, stateMap)

    ).flatten
  }
}

trait ModelMaking {
  def makeResource(name: String, state: List[String], abilities: List[(String, List[String])]) = {
    val t = Thing(name)
    val stateVars = Thing(s"$name.mode", SPAttributes("variableType"->"state")) :: state.map(x => Thing(s"$name.$x", SPAttributes("variableType"->"state")))
    val ab = abilities.map{case (n, params) =>
      val parameters = params ++ List("run","mode") // all abilities have these
    val abilityName = name +"."+n
      val o = Operation(abilityName, List(), SPAttributes("operationType"->"ability"))
      (o, parameters.map{x =>
        val pName = x.replaceFirst("p_", "")
        val isP = x.startsWith("p_")
        val attr = if (isP) "parameter" else "state"
        Thing(abilityName+"."+pName, SPAttributes("variableType"->attr))
      })
    }
    val abHir = ab.map{case (op, para) => HierarchyNode(op.id, para.map(p => HierarchyNode(p.id)))}
    val hier = HierarchyNode(t.id, stateVars.map(x => HierarchyNode(x.id)) ++ abHir)
    val temp: List[IDAble] = t :: stateVars ++ ab.map(_._1) ++ ab.flatMap(_._2)

    (hier, temp)
  }

  case class OpTowerTypeDef(name: String, op: String, pick: List[Int], place: List[Int], ability: ID, parameter: ID)
  def makeMeOperationTypes(xs: List[OpTowerTypeDef]): List[IDAble] = {
    xs.map{x =>
      val b = SPAttributes(
        "op" -> x.op,
        "ability" -> x.ability,
        "parameter" -> x.parameter
      ) + {if (x.pick.nonEmpty) SPAttributes("pick"->x.pick) else SPAttributes()} +
        {if (x.place.nonEmpty) SPAttributes("place"->x.place) else SPAttributes()}
      Operation(x.name, List(), SPAttributes(
        "operationType"->"operation",
        "behavior" -> b
      ))
    }
  }


}







