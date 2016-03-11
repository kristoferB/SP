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
      val r2 = makeResource(
        name = "r2",
        state = List("position", "mode"),// the state variables defining the state of the resource
        abilities = List(
          "movePaletteToStock" -> List("mode"),
          "movePaletteToFlexlink" -> List("mode"),
          "moveFixtureToBuildingPlace" -> List("mode"),
          "moveTowerToTable" -> List("mode")
        )
      )

      val r5 = makeResource(
        name = "r5",
        state = List("position", "mode"),
        abilities = List(
          "gripping" -> List("mode"),
          "moveToStart" -> List("start_parameter", "mode"),
          "moveToEnd" -> List("end_parameter", "mode")
        )
      )

      val r4 = makeResource(
        name = "r4",
        state = List("position", "mode"),
        abilities = List(
          "gripping" -> List("mode"),
          "moveToStart" -> List("start_parameter", "mode"),
          "moveToEnd" -> List("end_parameter", "mode")
        )
      )

      val s1 = makeResource(
        name = "s1",
        state = List("stop", "mode"),
        abilities = List("open"->List("mode"), "close"->List("mode"))
      )

      val s2 = makeResource(
        name = "s2",
        state = List("stop", "mode"),
        abilities = List("open"->List("mode"), "close"->List("mode"))
      )

      val s3 = makeResource(
        name = "s3",
        state = List("stop", "mode"),
        abilities = List("open"->List("mode"), "close"->List("mode"))
      )

      val s4 = makeResource(
        name = "s4",
        state = List("stop", "mode"),
        abilities = List("open"->List("mode"), "close"->List("mode"))
      )

      val flexLink = makeResource (
        name = "flexlink",
        state = List("mode"),
        abilities = List("run"->List("mode"))
      )

      val h1 = makeResource (
        name = "h1",
        state = List("mode"),
        abilities = List("up"->List("mode"), "down"->List("mode"))
      )


      val items = r2._2 ++ r4._2 ++ r5._2 ++ s1._2 ++ s2._2 ++ s3._2 ++ s4._2 ++ flexLink._2 ++ h1._2 ++ longList
      val itemMap = items.map(x => x.name -> x.id) toMap
      val stateMap = Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")

      // This info will later on be filled by a service on the bus
      val connectionList = List(
        //robot r2, siffror ska ändras senare
        db(itemMap, "r2.movePaletteToStock",      "bool",   950, 4, 1),
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

        db(itemMap, "h1.up", "bool", 135, 0, 0),
        db(itemMap, "h1.down", "bool", 135, 0, 1),
        db(itemMap, "h1.mode", "int", 135, 2, 0, stateMap)

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

      val root = HierarchyRoot("Resources", List(r2._1, r4._1, r5._1, s1._1, s2._1, s3._1, s4._1, flexLink._1, h1._1, HierarchyNode(sopSpec.id)))
      //val opRoot = HierarchyRoot("Operations", List())
      replyTo ! Response(items :+ root :+ connection, SPAttributes("info"->"Items created from PSLModel service"), rnr.req.service, rnr.req.reqID)

    }
  }
}

case class DBConnection(name: String, valueType: String, db: Int, byte: Int = 0, bit: Int = 0, intMap: Map[String, String] = Map(), id: ID = ID.newID)


trait ModelMaking {
  def makeResource(name: String, state: List[String], abilities: List[(String, List[String])]) = {
    val t = Thing(name)
    val stateVars = state.map(x => Thing(s"$name.$x", SPAttributes("variableType"->"state")))
    val ab = abilities.map{case (n, parameters) =>
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

  def db(items: Map[String, ID], name: String, valueType: String, db:Int, byte: Int, bit: Int, intMap: Map[Int, String] = Map()) = {
    items.get(name).map(id => DBConnection(name, valueType, db, byte, bit, intMap.map{case (k,v) => k.toString->v}, id))
  }
}






