package sp.psl

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._


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

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      // Resources
      val r2 = makeResource(
        name = "r2",
        state = List("position", "mode"),// the state variables defining the state of the resource
        abilities = List(
          "moveHomeToFixture"->List("p_aParameter", "mode", "mirror"), // the abilities of the resource. Includes the parameters as well as the state of the ability
          "moveFixtureToHome"->List("mode"))
      )

      val flS2 = makeResource(
        name = "flexlinkS2",
        state = List("stopper", "mode"),
        abilities = List("open"->List("mode"), "close"->List("mode"))
      )


      val items = r2._2 ++ flS2._2
      val itemMap = items.map(x => x.name -> x.id) toMap

      // This info will later on be filled by a service on the bus
      val connectionList = List(
        db(itemMap, "r2.moveHomeToFixture",       "bool",    950, 0, 0),
        db(itemMap, "r2.moveHomeToFixture.aParameter", "bool",    950, 0, 1),
        db(itemMap, "r2.moveHomeToFixture.mode",  "int",    950, 4, 0, Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")),
        db(itemMap, "r2.moveHomeToFixture.mirror", "bool",    950, 0, 5),
//        db(itemMap, "r2.moveFixtureToHome",       "bool",    950, 0, 1),
//        db(itemMap, "r2.moveFixtureToHome.mode",  "int",    950, 4, 0, Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")),
//        db(itemMap, "r2.moveFixtureToHome.speed", "int",    950, 2, 0),
        db(itemMap, "r2.position",                "int",    950, 4, 0, Map(0->"home", 1->"atFlexlink", 2->"atFixture")),
        db(itemMap, "r2.mode",                    "int",    950, 4, 0, Map(0->"notReady", 1->"ready", 2->"executing")) //,
//        db(itemMap, "flexlinkS2.open",            "bool",    950, 0, 2),
//        db(itemMap, "flexlinkS2.open.mode",       "bool",    950, 0, 6),
//        db(itemMap, "flexlinkS2.close",           "bool",    950, 0, 3),
//        db(itemMap, "flexlinkS2.close.mode",      "int",        950, 4, 0, Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")),
//        db(itemMap, "flexlinkS2.stopper",         "bool",    950, 0, 6),
//        db(itemMap, "flexlinkS2.mode",            "int",    950, 4, 0, Map(0->"notReady", 1->"ready", 2->"executing"))
      ).flatten

      val connection = SPSpec("PLCConnection", SPAttributes(
        "connection"->connectionList,
        "specification"-> "PLCConnection"
      ))

      val root = HierarchyRoot("Resources", List(r2._1, flS2._1))


      // Here you can make the operations
      // Look into the Operation class in domain
      // use PropositionParser to parse strings into guards and actions.
      // make a function that makes the operations like the makeResource
      // to simplify your modeling.
      // send in items to the parser
      // incl all operations in response.


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

  def db(items: Map[String, ID], name: String, valueType: String, db: Int, byte: Int = 0, bit: Int = 0, intMap: Map[Int, String] = Map()) = {
    items.get(name).map(id => DBConnection(name, valueType, db, byte, bit, intMap.map{case (k,v) => k.toString->v}, id))
  }

}






