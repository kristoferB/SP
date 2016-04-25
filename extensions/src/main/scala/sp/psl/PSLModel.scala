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

class PSLModel extends Actor with ServiceSupport with ModelMaking {
  import context.dispatcher
  //val other = context.actorOf(Props[RunnerService], "OperationControl")

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      val o1 = Operation("o1", List(), SPAttributes(), ID.makeID("a0f565e2-e44b-4017-a24e-c7d01e970dec").get)
      val o2 = Operation("o2",List(), SPAttributes(), ID.makeID("b0f565e2-e44b-4017-a24e-c7d01e970dec").get)
      val o3 = Operation("o3",List(), SPAttributes(), ID.makeID("c0f565e2-e44b-4017-a24e-c7d01e970dec").get)
      val o4 = Operation("o4",List(), SPAttributes(), ID.makeID("d0f565e2-e44b-4017-a24e-c7d01e970dec").get)
      val o5 = Operation("o5",List(), SPAttributes(), ID.makeID("e0f565e2-e44b-4017-a24e-c7d01e970dec").get)
      val sop = Parallel(Sequence(o1, Parallel(Sequence(o2, o3), o4), o5))

      val sopSpec =  SOPSpec("theSOPSpec", List(sop), SPAttributes())

      val longList: List[IDAble] = List(o1, o2, o3, o4, o5, sopSpec)
      /*
            val x = Request("RunnerService",
              SPAttributes(
                "SOP" -> sopSpec.id
              ),
              longList
            )

            other ! x
      */

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
        abilities = List("run"->List())
      )

      //skicka klossplatta till hiss in
      val toRobo = makeResource (
        name = "toRobo",
        state = List("mode"),
        abilities = List("run"->List())
      )

      val R5 = makeResource (
        name = "R5",
        state = List("mode"),
        abilities = List("pickBlock"->List(), "placeBlock"->List(),
        "toHome"->List(), "toDodge"->List())
      )

      val R4 = makeResource (
        name = "R4",
        state = List("mode"),
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


      val items = h2._2 ++ h3._2 ++ toOper._2 ++ toRobo._2 ++ R5._2 ++ R4._2 ++ R2._2 ++ longList ++ sensorIH2._2
      val itemMap = items.map(x => x.name -> x.id).toMap
      val stateMap = Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")

      // This info will later on be filled by a service on the bus
      val connectionList = List(

        //robot r2, siffror ska ändras senare
        db(itemMap, "r2.movePaletteToStock",      "bool",   950, 0, 0),
        db(itemMap, "r2.movePaletteToStock.mode", "int",    950, 4, 0, stateMap),
        db(itemMap, "r2.movePaletteToFlexlink",   "bool",   950, 0, 1),
        db(itemMap, "r2.movePaletteToFlexlink.mode", "int", 950, 0, 13, stateMap),
        db(itemMap, "r2.moveFixtureToBuildingPlace", "bool", 950, 1, 1),
        db(itemMap, "r2.moveFixtureToBuildingPlace.mode", "int", 950, 1, 2, stateMap),
        db(itemMap, "r2.moveTowerToTable",        "bool",   950, 2, 0),
        db(itemMap, "r2.moveTowerToTable.mode",   "int",    950, 2, 1, stateMap),
        db(itemMap, "r2.position",                "int",    950, 4, 0, Map(0->"home", 1->"atH1", 2->"atH2", 3->"atLBP", 4->"atBP",
          5->"atLR4", 6->"atLR5", 7->"atLP")),
        db(itemMap, "r2.mode",                    "int",    950, 4, 0, stateMap),


        // r5-robot som är detsamma som r4, siffror ska ändras senare
          db(itemMap, "r5.gripping",                  "bool",   999, 0, 0), // ändras då den ska greppa någonting
          db(itemMap, "r5.gripping.mode",             "int",    999, 0, 2, Map(0->"notReady", 3->"completed")),
          db(itemMap, "r5.moveToStart",               "bool",   998, 0, 0), // ändras då den ska flytta sig till start
          db(itemMap, "r5.moveToStart.start_parameter", "bool", 998, 0, 1), // om vi har fått in en startparameter eller inte
          db(itemMap, "r5.moveToStart.mode",          "int",    998, 0, 2, stateMap),
          db(itemMap, "r5.moveToEnd",                 "bool",  997, 0, 0),
          db(itemMap, "r5.moveToEnd.end_parameter",   "bool",   997, 0, 1),
          db(itemMap, "r5.moveToEnd.mode",            "int",    997, 0, 2, stateMap),
          db(itemMap, "r5.position",                  "int",    996, 0, 0, Map(0->"home", 1->"atLR4", 2->"atLR5", 3->"atBP")),
          db(itemMap, "r5.mode",                      "int",    996, 0, 1, stateMap),


        //r4-robot som är detsamma som r5, siffror ska ändras senare'
        db(itemMap, "r4.gripping",                  "bool",   999, 0, 0), // ändras då den ska greppa någonting
        db(itemMap, "r4.gripping.mode",             "int",    999, 0, 2, Map(0->"notReady", 3->"completed")),
        db(itemMap, "r4.moveToStart",               "bool",   998, 0, 0), // ändras då den ska flytta sig till start
        db(itemMap, "r4.moveToStart.start_parameter", "bool", 998, 0, 1), // om vi har fått in en startparameter eller inte
        db(itemMap, "r4.moveToStart.mode",          "int",    998, 0, 2, stateMap),
        db(itemMap, "r4.moveToEnd",                 "bool",  997, 0, 0),
        db(itemMap, "r4.moveToEnd.end_parameter",   "bool",   997, 0, 1),
        db(itemMap, "r4.moveToEnd.mode",            "int",    997, 0, 2, stateMap),
        db(itemMap, "r4.position",                  "int",    996, 0, 0, Map(0->"home", 1->"atLR4", 2->"atLR5", 3->"atBP")),
        db(itemMap, "r4.mode",                      "int",    996, 0, 1, stateMap),



        //stoppet där köbildning av paletter kommer skapas
        db(itemMap,"s1.open",             "bool", 950, 1, 2),
        db(itemMap, "s1.open.mode",       "int", 950, 1, 3, stateMap),
        db(itemMap, "s1.close",           "bool", 950, 1, 4),
        db(itemMap, "s1.close.mode",       "bool", 950, 1, 5),
        db(itemMap, "s1.stop",             "bool", 950, 2, 1),
        db(itemMap, "s1.mode",            "int", 950, 2, 2, Map(0->"open", 1->"closed")),


        //stoppet vid operatören
        db(itemMap,"s2.open",             "bool", 950, 1, 2),
        db(itemMap, "s2.open.mode",       "int", 950, 1, 3, stateMap),
        db(itemMap, "s2.close",           "bool", 950, 1, 4),
        db(itemMap, "s2.close.mode",       "bool", 950, 1, 5),
        db(itemMap, "s2.stop",             "bool", 950, 2, 1),
        db(itemMap, "s2.mode",            "int", 950, 2, 2, Map(0->"open", 1->"closed")),


        // stoppet vid hiss1
        db(itemMap,"s3.open",             "bool", 950, 1, 2),
        db(itemMap, "s3.open.mode",       "int", 950, 1, 3, stateMap),
        db(itemMap, "s3.close",           "bool", 950, 1, 4),
        db(itemMap, "s3.close.mode",      "bool", 950, 1, 5),
        db(itemMap, "s3.stop",            "bool", 950, 2, 1),
        db(itemMap, "s3.mode",            "int", 950, 2, 2, Map(0->"open", 1->"closed")),


        //stoppet vid hiss2
        db(itemMap, "s4.open",            "bool", 950, 1, 2),
        db(itemMap, "s4.open.mode",       "int", 950, 1, 3, stateMap),
        db(itemMap, "s4.close",           "bool", 950, 1, 4),
        db(itemMap, "s4.close.mode",       "bool", 950, 1, 5),
        db(itemMap, "s4.stop",             "bool", 950, 2, 1),
        db(itemMap, "s4.mode",            "int", 950, 2, 2, Map(0->"open", 1->"closed")),


        //flexlinkbandet, ihopblandat!!
        db(itemMap, "flexlink.run",       "bool", 111, 0, 0),
        db(itemMap, "flexlink.run.mode",  "bool", 111, 0, 1, stateMap),
        db(itemMap, "flexLink.mode",      "bool", 111, 0, 2),


        db(itemMap, "h2.up.run", "bool", 135, 0, 0),
        db(itemMap, "h2.down.run", "bool", 135, 0, 1),
        db(itemMap, "h2.up.mode", "int", 135, 2, 0, stateMap),
        db(itemMap, "h2.down.mode", "int", 135, 4, 0, stateMap),

        db(itemMap, "h3.up.run", "bool", 140, 0, 0),
        db(itemMap, "h3.down.run", "bool", 140, 0, 1),
        db(itemMap, "h3.up.mode", "int", 140, 2, 0, stateMap),
        db(itemMap, "h3.down.mode", "int", 140, 4, 0, stateMap),

        db(itemMap, "toOper.run", "bool", 139, 0, 0),
        db(itemMap, "toOper.mode", "int", 139, 2, 0, stateMap),
        db(itemMap, "toRobo.run", "bool", 139, 0, 1),
        db(itemMap, "toRobo.mode", "int", 139, 4, 0, stateMap),

        db(itemMap, "R5.pickBlock.run", "bool", 128, 0, 0),
        db(itemMap, "R5.pickBlock.mode", "int", 128, 2, 0, stateMap),
        db(itemMap, "R5.placeBlock.run", "bool", 128, 0, 1),
        db(itemMap, "R5.placeBlock.mode", "int", 128, 4, 0, stateMap),
        db(itemMap, "R5.toHome.run", "bool", 128, 0, 2),
        db(itemMap, "R5.toHome.mode", "int", 128, 6, 0, stateMap),
        db(itemMap, "R5.toDodge.run", "bool", 128, 0, 3),
        db(itemMap, "R5.toDodge.mode", "int", 128, 8, 0, stateMap),

        db(itemMap, "R4.pickBlock.run", "bool", 128, 0, 0),
        db(itemMap, "R4.pickBlock.mode", "int", 128, 2, 0, stateMap),
        db(itemMap, "R4.placeBlock.run", "bool", 128, 0, 1),
        db(itemMap, "R4.placeBlock.mode", "int", 128, 4, 0, stateMap),
        db(itemMap, "R4.toHome.run", "bool", 128, 0, 2),
        db(itemMap, "R4.toHome.mode", "int", 128, 6, 0, stateMap),
        db(itemMap, "R4.toDodge.run", "bool", 128, 0, 3),
        db(itemMap, "R4.toDodge.mode", "int", 128, 8, 0, stateMap),

        db(itemMap, "elevatorStn2ToHomeTable.run", "bool",126,0,0),
        db(itemMap, "homeTableToHomeBP.run", "bool",126,0,1),
        db(itemMap, "homeTableToElevatorStn3.run", "bool",126,0,2),
        db(itemMap, "homeBPToHomeTable.run", "bool",126,0,3),
        db(itemMap, "placeAtPos.run", "bool",126,0,4),
        db(itemMap, "pickAtPos.run", "bool",126,0,5),
        db(itemMap, "elevatorStn2ToHomeTable.mode", "int", 216, 2, 0, stateMap),
        db(itemMap, "homeTableToHomeBP.mode", "int", 126, 4, 0, stateMap),
        db(itemMap, "homeTableToElevatorStn3.mode", "int", 126, 6, 0, stateMap),
        db(itemMap, "homeBPToHomeTable.mode", "int", 126, 8, 0, stateMap),
        db(itemMap, "placeAtPos.run", "int", 126, 10, 0, stateMap),
        db(itemMap, "pickAtPos.run", "int", 126, 12, 0, stateMap),

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


      //import sp.domain.logic.PropositionParser._
      //operations exempel

      val root = HierarchyRoot("Resources", List(h2._1, h3._1, toOper._1, toRobo._1, R5._1, R4._1, R2._1, sensorIH2._1, HierarchyNode(sopSpec.id)))
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


  //behöver denna under tiden vi hårkodar och testar en SOP, kommer inte behövas senare
  /*var sopID: Map[String, ID] = Map()
  def addSopSpecID(sopSpec: SOPSpec, sopName: String) ={
    sopID = sopID + (sopName -> sopSpec.id)
  }*/

  /*

    def makeOperation(opName: String, itemMap: Map[String, ID], madeOfAbilities: List[String])={
      val name = Operation(opName)
      val attributes = SPAttributes()
      for(ability <- madeOfAbilities){
        attributes ++ SPAttributes("ability" -> itemMap(ability))
      }
      //val op = Operation(opName, List(), attributes)
      //val abil = madeOfAbilities.map(x => Thing(s"$name.$x", SPAttributes("variableType"->"abilities")))
      //val hier = HierarchyNode(op.id, abil.map(x => HierarchyNode(x.id)))
      //val temp: List[IDAble] = op :: abil
      //(hier, temp)
    }*/

  def makeOperation(opName: String, itemMap: Map[String, ID], madeOfAbilities: List[String])={
    val name = opName
    val attributes = SPAttributes()
    for(ability <- madeOfAbilities){
      attributes ++ SPAttributes("ability" -> itemMap(ability))
    }
    val op = Operation(name, List(), attributes)
    //val abil = madeOfAbilities.map(x => Thing(s"$name.$x", SPAttributes("variableType"->"abilities")))
    //val hier = HierarchyNode(op.id, abil.map(x => HierarchyNode(x.id)))
    //val temp: List[IDAble] = op :: abil
    //(hier, temp)
  }


  def db(items: Map[String, ID], name: String, valueType: String, db:Int, byte: Int, bit: Int, intMap: Map[Int, String] = Map()) = {
    items.get(name).map(id => DBConnection(name, valueType, db, byte, bit, intMap.map{case (k,v) => k.toString->v}, id))
  }
}






