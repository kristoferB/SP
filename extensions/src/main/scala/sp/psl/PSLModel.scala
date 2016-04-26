package sp.psl

import akka.actor._
import sp.control.AddressValues
import sp.domain.logic.{PropositionParser, ActionParser, IDAbleLogic}
import sp.runnerService.RunnerService
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

case class AbilityStructure(name: String, parameter: Option[Int])

class PSLModel extends Actor with ServiceSupport with ModelMaking {
  import context.dispatcher
  //val other = context.actorOf(Props[RunnerService], "OperationControl")

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get


      val o1 = Operation("h2", List(), SPAttributes("ability"-> AbilityStructure("h2.up.run", Some(-1))))
      val o2 = Operation("h2",List(), SPAttributes("ability"-> AbilityStructure("h2.down.run", Some(-1))))
      val o3 = Operation("h3",List(), SPAttributes("ability"-> AbilityStructure("h3.up.run", Some(-1))))
      val o4 = Operation("h3",List(), SPAttributes("ability"-> AbilityStructure("h3.down.run", Some(-1))))
      val o5 = Operation("R2.elevatorStn2ToHomeTable.run", List(), SPAttributes("ability"-> AbilityStructure("R2.elevatorStn2ToHomeTable.run", Some(-1))))
      val sop = Parallel(Sequence(o1, Sequence(o2, o3), o4))

      val sopSpec =  SOPSpec("theSOPSpec", List(sop), SPAttributes())

      val longList: List[IDAble] = List(o1, o2, o3, o4, o5, sopSpec)

      //test
      val op1 = Operation("h1.up.run", List(), SPAttributes("ability"-> AbilityStructure("h1.up.run", Some(-1))), ID.makeID("a0f565e2-e44b-4017-a24e-c7d01e970dec").get)
      val op2 = Operation("h1.down.run", List(), SPAttributes("ability"-> AbilityStructure("h1.down.run", Some(-1))))
      val op3 = Operation("h4.up.run", List(), SPAttributes("ability"-> AbilityStructure("h4.up.run", Some(-1))))
      val op4 = Operation("h4.down.run", List(), SPAttributes("ability"-> AbilityStructure("h4.down.run", Some(-1))))
      val aSOP = Parallel(Sequence(op1, Parallel(Sequence(op2, op3), op4)))

      val thaSOP = SOPSpec("thaSOP", List(aSOP), SPAttributes())

      val aList: List[IDAble] = List(op1, op2, op3, op4, thaSOP)

      // Resources

      val h2 = makeResource (
        name = "h2",
        state = List("mode"),
        abilities = List("up"->List(), "down"->List())
      )
      val h3 = makeResource (
        name = "h3",
        state = List("mode"),
        abilities = List("up"->List(), "down"->List())
      )

      //skicka klossplatta till operatör
      val toOper = makeResource (
        name = "toOper",
        state = List("mode"),
        abilities = List("move"-> List())
      )

      //skicka klossplatta till hiss in
      val toRobo = makeResource (
        name = "toRobo",
        state = List("mode"),
        abilities = List("move" -> List())
      )

      val R5 = makeResource (
        name = "R5",
        state = List("mode"),
        abilities = List("pickBlock"->List(), "placeBlock"->List(),
        "toHome"->List(), "toDodge"->List())
      )

      val R4 = makeResource (
        name = "R4",
        state = List("mode", "parameters"),
        abilities = List("pickBlock"->List(), "placeBlock"->List(),
          "toHome"->List(), "toDodge"->List())
      )

      val R2 = makeResource (
        name = "R2",
        state = List("mode"),
        abilities = List("elevatorStn2ToHomeTable"->List(), "homeTableToHomeBP" ->List(),
          "homeTableToElevatorStn3"->List(), "homeBPToHomeTable"->List(),
          "placeAtPos"->List(), "pickAtPos"->List())
      )

      val sensorIH2 = makeResource (
        name = "IH2",
        state = List("mode"),
        abilities = List()
      )

      // test
      val h1 = makeResource (
        name = "h1",
        state = List("mode"),
        abilities = List("up"->List(), "down"->List())
      )
      val h4 = makeResource (
        name = "h4",
        state = List("mode"),
        abilities = List("up"->List(), "down"->List())
      )

      val items = h2._2 ++ h3._2 ++ toOper._2 ++ toRobo._2 ++ R5._2 ++ R4._2 ++ R2._2 ++ longList ++ sensorIH2._2 ++ aList ++ h1._2 ++ h4._2
      val itemMap = items.map(x => x.name -> x.id).toMap
      val stateMap = Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")

      // This info will later on be filled by a service on the bus
      val connectionList = List(
        db(itemMap, "h2.up.run", "bool", 135, 0, 0),
        db(itemMap, "h2.down.run", "bool", 135, 0, 1),
        db(itemMap, "h2.up.mode", "int", 135, 2, 0, stateMap),
        db(itemMap, "h2.down.mode", "int", 135, 4, 0, stateMap),

        db(itemMap, "h3.up.run", "bool", 140, 0, 0),
        db(itemMap, "h3.down.run", "bool", 140, 0, 1),
        db(itemMap, "h3.up.mode", "int", 140, 2, 0, stateMap),
        db(itemMap, "h3.down.mode", "int", 140, 4, 0, stateMap),

        //test
        db(itemMap, "h1.up.run", "bool", 755, 8, 0),
        db(itemMap, "h1.down.run", "bool", 755, 8, 1),
        db(itemMap, "h1.up.mode", "int", 755, 0, 0, stateMap),
        db(itemMap, "h1.down.mode", "int", 755, 2, 0, stateMap),

        db(itemMap, "h4.up.run", "bool", 755, 8, 2),
        db(itemMap, "h4.down.run", "bool", 755, 8, 3),
        db(itemMap, "h4.up.mode", "int", 755, 4, 0, stateMap),
        db(itemMap, "h4.down.mode", "int", 755, 6, 0, stateMap),
        //

        db(itemMap, "toOper.run", "bool", 139, 0, 0),
        db(itemMap, "toOper.mode", "int", 139, 2, 0, stateMap),
        db(itemMap, "toRobo.run", "bool", 139, 0, 1),
        db(itemMap, "toRobo.mode", "int", 139, 4, 0, stateMap),

        db(itemMap, "R5.pickBlock.run", "bool", 132, 0, 0),
        db(itemMap, "R5.pickBlock.mode", "int", 132, 2, 0, stateMap),
        db(itemMap, "R5.placeBlock.run", "bool", 132, 0, 1),
        db(itemMap, "R5.placeBlock.mode", "int", 132, 4, 0, stateMap),
        db(itemMap, "R5.toHome.run", "bool", 132, 0, 2),
        db(itemMap, "R5.toHome.mode", "int", 132, 6, 0, stateMap),
        db(itemMap, "R5.toDodge.run", "bool", 132, 0, 3),
        db(itemMap, "R5.toDodge.mode", "int", 132, 8, 0, stateMap),

        db(itemMap, "R4.pickBlock.run", "bool", 128, 0, 0),
        db(itemMap, "R4.pickBlock.mode", "int", 128, 2, 0, stateMap),
        db(itemMap, "R4.placeBlock.run", "bool", 128, 0, 1),
        db(itemMap, "R4.placeBlock.mode", "int", 128, 4, 0, stateMap),
        db(itemMap, "R4.toHome.run", "bool", 128, 0, 2),
        db(itemMap, "R4.toHome.mode", "int", 128, 6, 0, stateMap),
        db(itemMap, "R4.toDodge.run", "bool", 128, 0, 3),
        db(itemMap, "R4.toDodge.mode", "int", 128, 8, 0, stateMap),
        db(itemMap, "R4.parameters", "int", 128, 10, 0),

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
        db(itemMap, "R2.pickAtPos.run", "int", 126, 12, 0, stateMap),

        db(itemMap, "IH2.mode", "bool", 755, 8, 4)

      ).flatten

      val connection = SPSpec("PLCConnection", SPAttributes(
        "connection"->connectionList,
        "specification"-> "PLCConnection"
      ))

      // Here you can make the operations
      // Look into the Operation class in domain
      // use PropositionParser to parse strings into guards and actions.q
      // make a function that makes the operations like the makeResource
      // to simplify your modeling.
      // send in items to the parser
      // incl all operations in response.


      val root = HierarchyRoot("Resources", List(h2._1, h3._1, toOper._1, toRobo._1, R5._1, R4._1, R2._1, h1._1, h4._1, sensorIH2._1, HierarchyNode(sopSpec.id), HierarchyNode(thaSOP.id)))
      replyTo ! Response(items :+ root :+ connection, SPAttributes("info"->"Items created from PSLModel service"), rnr.req.service, rnr.req.reqID)
    }
  }
}

case class DBConnection(name: String, valueType: String, db: Int, byte: Int = 0, bit: Int = 0, intMap: Map[String, String] = Map(), id: ID = ID.newID)


trait ModelMaking {
  def makeResource(name: String, state: List[String], abilities: List[(String, List[String])]) = {
    val t = Thing(name)
    val stateVars = state.map(x => Thing(s"$name.$x", SPAttributes("variableType"->"state")))
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


  def db(items: Map[String, ID], name: String, valueType: String, db:Int, byte: Int, bit: Int, intMap: Map[Int, String] = Map()) = {
    items.get(name).map(id => DBConnection(name, valueType, db, byte, bit, intMap.map{case (k,v) => k.toString->v}, id))
  }
}






