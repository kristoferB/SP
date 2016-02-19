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
      val r2 = makeResource("r2",
        List("position", "mode"),// the state variables defining the state of the resource
        List(
          "moveHomeToFixture"->List("mode", "speed"), // the abilities of the resource. Includes the parameters as well as the state of the ability
          "moveFixtureToHome"->List("mode"))
      )

      val flS2 = makeResource("flexlinkS2",
        List("stopper", "mode"),
        List("open"->List("mode"), "close"->List("mode"))
      )


      val items = r2._2 ++ flS2._2
      val itemMap = items.map(x => x.name -> x.id) toMap

      // This info will later on be filled by a service on the bus
      val connectionList = List(
        db(itemMap, "r2.moveHomeToFixture",       "boolean",    109, 0, 0),
        db(itemMap, "r2.moveHomeToFixture.mode",  "integer",    109, 2, 0, Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed"))
//        db(itemMap, "r2.moveFixtureToHome",       "boolean",    109, 0, 0),
//        db(itemMap, "r2.moveFixtureToHome.mode",  "integer",    109, 0, 0, Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")),
//        db(itemMap, "r2.moveFixtureToHome.speed", "integer",    109, 0, 0),
//        db(itemMap, "r2.position",                "integer",    109, 0, 0, Map(0->"home", 1->"atFlexlink", 2->"atFixture")),
//        db(itemMap, "r2.mode",                    "integer",    109, 0, 0, Map(0->"notReady", 1->"ready", 2->"executing")),
//        db(itemMap, "flexlinkS2.open",            "boolean",    109, 0, 0),
//        db(itemMap, "flexlinkS2.open.mode",       "integer",    109, 0, 0, Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")),
//        db(itemMap, "flexlinkS2.close",           "boolean",    109, 0, 0),
//        db(itemMap, "flexlinkS2.close.mode",      "Int",        109, 0, 0, Map(0->"notReady", 1->"ready", 2->"executing", 3->"completed")),
//        db(itemMap, "flexlinkS2.stopper",         "boolean",    109, 0, 0),
//        db(itemMap, "flexlinkS2.mode",            "integer",    109, 0, 0, Map(0->"notReady", 1->"ready", 2->"executing"))
      ).flatten

      val connection = Thing("PLCConnection", SPAttributes(
        "connection"->connectionList
      ))

      val root = HierarchyRoot("Resources", List(r2._1, flS2._1))


      replyTo ! Response(items :+ root :+ connection, SPAttributes("info"->"Items created from PSLModel service"), rnr.req.service, rnr.req.reqID)

    }
  }
}


case class DBConnection(name: String, valueType: String, db: Int, byte: Int = 0, bit: Int = 0, intMap: Map[Int, String] = Map(), id: ID = ID.newID)

trait ModelMaking {
  def makeResource(name: String, state: List[String], abilities: List[(String, List[String])]) = {
    val t = Thing(name)
    val stateVars = state.map(x => Thing(s"$name.$x"))
    val ab = abilities.map{case (n, parameters) =>
      val abilityName = name +"."+n
      val o = Operation(abilityName, List(), SPAttributes("operationType"->"ability"))
      (o, parameters.map(x => Thing(abilityName+"."+x)))
    }
    val abHir = ab.map{case (op, para) => HierarchyNode(op.id, para.map(p => HierarchyNode(p.id)))}
    val hier = HierarchyNode(t.id, stateVars.map(x => HierarchyNode(x.id)) ++ abHir)
    val temp: List[IDAble] = t :: stateVars ++ ab.map(_._1) ++ ab.flatMap(_._2)

    (hier, temp)
  }

  def db(items: Map[String, ID], name: String, valueType: String, db: Int, byte: Int = 0, bit: Int = 0, intMap: Map[Int, String] = Map()) = {
    items.get(name).map(id => DBConnection(name, valueType, db, byte, bit, intMap, id))
  }

}






